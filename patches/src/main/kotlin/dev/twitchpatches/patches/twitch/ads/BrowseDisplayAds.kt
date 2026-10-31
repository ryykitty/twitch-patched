package dev.twitchpatches.patches.twitch.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import dev.twitchpatches.patches.twitch.shared.*

internal data class BrowseNoFill(val empty: MethodReference, val single: MethodReference)

internal fun browseNoFill(method: Method): BrowseNoFill {
    val code = method.code()
    return code.indices.mapNotNull { index ->
        val empty = (code[index] as? ReferenceInstruction)?.reference as? MethodReference ?: return@mapNotNull null
        if (code[index].opcode != Opcode.INVOKE_STATIC ||
            empty.definingClass != $$"Ltv/twitch/android/util/Optional$Companion;" ||
            empty.parameterTypes.isNotEmpty() || empty.returnType != "Ltv/twitch/android/util/Optional;")
            return@mapNotNull null
        val move = code.getOrNull(index + 1) as? OneRegisterInstruction ?: return@mapNotNull null
        val call = code.getOrNull(index + 2) as? FiveRegisterInstruction ?: return@mapNotNull null
        val single = (call as? ReferenceInstruction)?.reference as? MethodReference ?: return@mapNotNull null
        val result = code.getOrNull(index + 3) as? OneRegisterInstruction ?: return@mapNotNull null
        val ret = code.getOrNull(index + 4) as? OneRegisterInstruction ?: return@mapNotNull null
        if (move.opcode != Opcode.MOVE_RESULT_OBJECT || call.opcode != Opcode.INVOKE_STATIC ||
            call.registerCount != 1 || call.registerC != move.registerA ||
            single.definingClass != "Lio/reactivex/u;" || single.parameterTypes != listOf("Ljava/lang/Object;") ||
            !single.returnType.startsWith("Lio/reactivex/internal/operators/") ||
            result.opcode != Opcode.MOVE_RESULT_OBJECT || ret.opcode != Opcode.RETURN_OBJECT ||
            result.registerA != ret.registerA) return@mapNotNull null
        BrowseNoFill(empty, single)
    }.uniqueHook("browse display-ad no-fill completion")
}

private object FollowingHeadlinerRequestFingerprint : Fingerprint(
    isStatic = false,
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf("show_headliner_ad", "ad_request_format_declined"),
    filters = listOf(
        methodCall(
            definingClass = $$"Ltv/twitch/android/util/Optional$Companion;",
            returnType = "Ltv/twitch/android/util/Optional;",
            opcode = Opcode.INVOKE_STATIC,
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            definingClass = "Lio/reactivex/u;",
            parameters = listOf("Ljava/lang/Object;"),
            opcode = Opcode.INVOKE_STATIC,
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal fun BytecodePatchContext.blockBrowseDisplayAds() {
    val request = FollowingHeadlinerRequestFingerprint.method
    val matches = FollowingHeadlinerRequestFingerprint.instructionMatches
    val empty = (matches[0].instruction as ReferenceInstruction).reference as MethodReference
    val single = (matches[2].instruction as ReferenceInstruction).reference as MethodReference
    if ((request.implementation?.registerCount ?: 0) <= 2)
        throw PatchException("Browse display ads: no local register at request entry.")
    request.addInstructionsWithLabels(0, """
        invoke-static {}, $AD_SETTINGS->displayAdsBlocked()Z
        move-result v0
        if-eqz v0, :original
        invoke-static {}, $empty
        move-result-object v0
        invoke-static {v0}, $single
        move-result-object v0
        return-object v0
    """, ExternalLabel("original", request.code().first()))
}
