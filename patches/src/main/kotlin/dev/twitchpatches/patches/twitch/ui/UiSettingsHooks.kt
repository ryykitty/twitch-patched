package dev.twitchpatches.patches.twitch.ui

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import dev.twitchpatches.patches.twitch.shared.*

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
    val models = mutableListOf<String>()
    classDefForEach { type ->
        if (type.methods.any { method -> method.references().filterIsInstance<StringReference>()
            .any { it.string.startsWith("StreamSettingsModel(configurablePlayer=") } }) models.add(type.type)
    }
    val model = models.uniqueHook("UI player settings model")
    val delegates = mutableListOf<String>()
    classDefForEach { type ->
        if (type.superclass == "Ltv/twitch/android/core/mvp/viewdelegate/RxViewDelegate;" &&
            type.methods.any { method -> method.references().filterIsInstance<FieldReference>().any {
                it.definingClass == model
            } }) delegates.add(type.type)
    }
    val owner = mutableClassDefBy(delegates.uniqueHook("UI player settings delegate"))
    val constructor = owner.methods.filter { it.name == "<init>" && it.returnType == "V" &&
        it.parameterTypes.size == 2 && it.parameterTypes[1] == "Landroid/view/View;" }
        .uniqueHook("UI player settings root constructor")
    constructor.code().withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }.asReversed().forEach {
        constructor.insertAtReturn(it,
            "invoke-static/range {p2 .. p2}, Ldev/twitchpatches/extension/settings/UiPlayerSettings;->bind(Landroid/view/View;)V")
    }
}

internal fun BytecodePatchContext.injectUiNavigation() =
    interceptReactNativeLink("twitchPatchesOpenUiSettings", "$UI_SETTINGS->openLink(Ljava/lang/String;)Z")

internal fun BytecodePatchContext.injectUiPolicyBridge() =
    generateReactNativeStringDispatch("dispatchUi", "TwitchPatchUi")
