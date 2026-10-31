package dev.twitchpatches.patches.twitch.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.string
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import dev.twitchpatches.patches.twitch.shared.*

private const val STRING = "Ljava/lang/String;"
private const val PROMISE = "Lcom/facebook/react/bridge/Promise;"
private const val INTENT = "Lcom/facebook/react/modules/intent/IntentModule;"
private const val RN = "Lcom/facebook/react/runtime/ReactInstance;"
private const val ARRAY = "Lcom/facebook/react/bridge/WritableNativeArray;"
private const val RUNTIME = "Ldev/twitchpatches/extension/shared/ReactNativeRuntime;"

private object UiStreamSettingsModelFingerprint : Fingerprint(
    strings = listOf(string("StreamSettingsModel(configurablePlayer=", StringComparisonType.STARTS_WITH)),
)

private class UiPlayerSettingsDelegateFingerprint(model: String) : Fingerprint(
    superclass = "Ltv/twitch/android/core/mvp/viewdelegate/RxViewDelegate;",
    filters = listOf(fieldAccess(definingClass = model)),
)

internal fun injectUiHeaderCapability(method: MutableMethod, available: Boolean) {
    val code = method.code()
    require(code.size == 2 && code[0].opcode == Opcode.CONST_4 && code[1].opcode == Opcode.RETURN &&
        (code[0] as OneRegisterInstruction).registerA == (code[1] as OneRegisterInstruction).registerA) {
        "UI settings: header capability stub changed."
    }
    requireNotNull(method.implementation).replaceInstruction(0,
        BuilderInstruction11n(Opcode.CONST_4, (code[0] as OneRegisterInstruction).registerA, if (available) 1 else 0))
}

internal fun BytecodePatchContext.injectUiPlayerEntry() {
    val model = UiStreamSettingsModelFingerprint.originalClassDef.type
    val owner = UiPlayerSettingsDelegateFingerprint(model).classDef
    val constructor = owner.methods.filter { it.name == "<init>" && it.returnType == "V" &&
        it.parameterTypes.size == 2 && it.parameterTypes[1] == "Landroid/view/View;" }
        .uniqueHook("UI player settings root constructor")
    constructor.code().withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }.asReversed().forEach {
        constructor.insertAtReturn(it,
            "invoke-static/range {p2 .. p2}, Ldev/twitchpatches/extension/settings/UiPlayerSettings;->bind(Landroid/view/View;)V")
    }
}

internal fun BytecodePatchContext.injectUiNavigation() {
    val owner = mutableClassDefBy(INTENT)
    val open = owner.methods.filter { it.name == "openURL" && it.isInstance(listOf(STRING, PROMISE), "V") }
        .uniqueHook("RN URL navigation")
    require(requireNotNull(open.implementation).registerCount >= 4) { "UI settings: URL entry has no scratch register." }
    classDefBy(PROMISE).methods.filter { it.name == "resolve" &&
        it.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/Object;") && it.returnType == "V" }
        .uniqueHook("RN navigation promise completion")
    val bridge = ImmutableMethod(INTENT, "twitchPatchesOpenUiSettings",
        listOf(STRING, PROMISE).map { ImmutableMethodParameter(it, null, null) }, "Z",
        AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, null, null, MutableMethodImplementation(4)).toMutable()
    require(owner.methods.none { it.name == bridge.name })
    bridge.addInstructionsWithLabels(0, """
        invoke-static/range {p0 .. p0}, $UI_SETTINGS->openLink($STRING)Z
        move-result v0
        if-eqz v0, :done
        const/4 v1, 0x0
        invoke-interface {p1, v1}, $PROMISE->resolve(Ljava/lang/Object;)V
        :done
        return v0
    """)
    owner.methods.add(bridge)
    injectUiUrlGate(open, bridge.reference)
}

internal fun injectUiUrlGate(open: MutableMethod, bridge: String) {
    open.addInstructionsWithLabels(0, """
        invoke-static/range {p1 .. p2}, $bridge
        move-result v0
        if-eqz v0, :original
        return-void
    """, ExternalLabel("original", open.getInstruction(0)))
}

internal fun BytecodePatchContext.injectUiPolicyBridge() {
    val call = classDefBy(RN).methods.filter { it.name == "callFunctionOnModule" && it.returnType == "V" &&
        it.parameterTypes.map(CharSequence::toString) == listOf(STRING, STRING, "Lcom/facebook/react/bridge/NativeArray;") }
        .uniqueHook("RN UI preference dispatch")
    classDefBy(ARRAY).methods.filter { it.name == "pushString" && it.returnType == "V" &&
        it.parameterTypes.map(CharSequence::toString) == listOf(STRING) && AccessFlags.PUBLIC.isSet(it.accessFlags) }
        .uniqueHook("RN UI preference writer")
    val owner = mutableClassDefBy(RUNTIME)
    val stub = owner.methods.filter { it.name == "dispatchUi" && it.returnType == "V" &&
        it.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/Object;", STRING) }
        .uniqueHook("extension UI policy bridge")
    val dispatch = ImmutableMethod(RUNTIME, stub.name, stub.parameters, "V", stub.accessFlags,
        stub.annotations, stub.hiddenApiRestrictions, MutableMethodImplementation(5)).toMutable()
    dispatch.addInstructionsWithLabels(0, """
        check-cast p0, $RN
        new-instance v0, $ARRAY
        invoke-direct {v0}, $ARRAY-><init>()V
        invoke-virtual {v0, p1}, $ARRAY->pushString($STRING)V
        const-string v1, "TwitchPatchUi"
        const-string v2, "set"
        invoke-virtual {p0, v1, v2, v0}, ${call.reference}
        return-void
    """)
    owner.methods.remove(stub)
    owner.methods.add(dispatch)
}
