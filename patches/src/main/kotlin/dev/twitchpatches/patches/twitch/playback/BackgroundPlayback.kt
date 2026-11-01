package dev.twitchpatches.patches.twitch.playback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import dev.twitchpatches.patches.twitch.shared.*

private const val VERTICAL = "Ltv/twitch/android/feature/theatre/vertical/VerticalTheatreFragment;"
private const val POLICY = "Ldev/twitchpatches/extension/playback/BackgroundPlayback;"

private object VerticalBackgroundEligibilityFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Z",
    parameters = listOf(VERTICAL),
    strings = listOf("audioDeviceManager"),
)

private object NativeDjFlagFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(definingClass = "Ltv/twitch/android/models/streams/StreamModel;", name = "getChannel"),
        methodCall(definingClass = "Ltv/twitch/android/models/channel/ChannelModel;", name = "isParticipatingDJ"),
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "valueOf"),
    ),
)

private object ClassicPlayerInactiveFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = "Lcom/comscore/Analytics;", name = "notifyExitForeground"),
        methodCall(definingClass = "Landroid/app/Activity;", name = "isFinishing"),
    ),
)

internal fun requireBackgroundEligibilityContract(lifecycle: Method, eligibility: Method) {
    val calls = lifecycle.references().filterIsInstance<MethodReference>().map { it.toString() }
    require(eligibility.reference in calls && eligibility.references().filterIsInstance<MethodReference>().count(::isDjStatus) == 1) {
        "Background playback: eligibility is no longer the DJ check used by the stop lifecycle."
    }
}

internal fun BytecodePatchContext.restoreNativeBackgroundPlayback() {
    val lifecycle = classDefBy(VERTICAL).methods.filter {
        it.name == "onStop" && it.parameterTypes.isEmpty() && it.returnType == "V"
    }.uniqueHook("vertical playback stop lifecycle")
    val eligibility = VerticalBackgroundEligibilityFingerprint.originalMethod
    requireBackgroundEligibilityContract(lifecycle, eligibility)
    val code = eligibility.code()
    val read = code.indexOfFirst { isDjStatus((it as? ReferenceInstruction)?.reference as? MethodReference) }
    val result = code[read + 1] as? OneRegisterInstruction
    require(code[read + 1].opcode == Opcode.MOVE_RESULT && result != null) {
        "Background playback: DJ result contract changed."
    }
    // Keep Twitch's audio-device preference and foreground-service lifecycle intact.
    VerticalBackgroundEligibilityFingerprint.method.replaceInstruction(read + 1, "const/16 v${result.registerA}, 0x0")
    applyNativeDjFlag()
    insertClassicPictureInPictureAudio()
}

private fun BytecodePatchContext.applyNativeDjFlag() {
    val code = NativeDjFlagFingerprint.originalMethod.code()
    val read = code.indexOfFirst { isDjStatus((it as? ReferenceInstruction)?.reference as? MethodReference) }
    val result = code[read + 1] as? OneRegisterInstruction
    require(code[read + 1].opcode == Opcode.MOVE_RESULT && result != null) { "Background playback: native DJ flag contract changed." }
    NativeDjFlagFingerprint.method.replaceInstruction(read + 1, "const/16 v${result.registerA}, 0x0")
}

private fun BytecodePatchContext.insertClassicPictureInPictureAudio() {
    val code = ClassicPlayerInactiveFingerprint.originalMethod.code()
    val flag = classicPictureInPictureFlag(code)
    val register = (code[flag] as TwoRegisterInstruction).registerA
    ClassicPlayerInactiveFingerprint.method.addInstructions(flag + 1, """
        invoke-static/range {v$register .. v$register}, $POLICY->pictureInPictureBlocksAudio(Z)Z
        move-result v$register
    """)
}

internal fun classicPictureInPictureFlag(code: List<Instruction>): Int {
    val finishing = code.indexOfFirst {
        ((it as? ReferenceInstruction)?.reference as? MethodReference)?.name == "isFinishing"
    }
    val flags = code.indices.filter { it > finishing && code[it].opcode == Opcode.IGET_BOOLEAN }
    require(finishing >= 0 && flags.size == 2 && code[flags[1] + 1].opcode == Opcode.IF_NEZ) {
        "Background playback: classic picture-in-picture check changed."
    }
    return flags[1]
}
