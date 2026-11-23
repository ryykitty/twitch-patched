package dev.twitchpatches.patches.twitch.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import dev.twitchpatches.patches.twitch.shared.*

private const val HEADLINER_PREFERENCE = "show_headliner_ad"

internal data class BrowseNoFill(val empty: MethodReference, val observable: MethodReference)

internal fun browseNoFill(method: Method): BrowseNoFill {
    val code = method.code()
    val addresses = code.runningFold(0) { address, instruction -> address + instruction.codeUnits }
    return code.indices.mapNotNull { index ->
        val empty = (code[index] as? ReferenceInstruction)?.reference as? MethodReference ?: return@mapNotNull null
        if (code[index].opcode != Opcode.INVOKE_STATIC ||
            empty.definingClass != $$"Ltv/twitch/android/util/Optional$Companion;" ||
            empty.parameterTypes.isNotEmpty() || empty.returnType != "Ltv/twitch/android/util/Optional;")
            return@mapNotNull null
        val move = code.getOrNull(index + 1) as? OneRegisterInstruction ?: return@mapNotNull null
        val call = code.getOrNull(index + 2) as? FiveRegisterInstruction ?: return@mapNotNull null
        val observable = (call as? ReferenceInstruction)?.reference as? MethodReference ?: return@mapNotNull null
        val result = code.getOrNull(index + 3) as? OneRegisterInstruction ?: return@mapNotNull null
        val next = code.getOrNull(index + 4) ?: return@mapNotNull null
        val ret = if (next.opcode in setOf(Opcode.GOTO, Opcode.GOTO_16, Opcode.GOTO_32)) {
            val address = addresses[index + 4] + (next as OffsetInstruction).codeOffset
            code.getOrNull(addresses.binarySearch(address))
        } else next
        if (move.opcode != Opcode.MOVE_RESULT_OBJECT || call.opcode != Opcode.INVOKE_STATIC ||
            call.registerCount != 1 || call.registerC != move.registerA ||
            !observable.definingClass.startsWith("Lio/reactivex/") || observable.parameterTypes != listOf("Ljava/lang/Object;") ||
            !observable.returnType.startsWith("Lio/reactivex/internal/operators/observable/") ||
            result.opcode != Opcode.MOVE_RESULT_OBJECT || ret !is OneRegisterInstruction || ret.opcode != Opcode.RETURN_OBJECT ||
            result.registerA != ret.registerA) return@mapNotNull null
        BrowseNoFill(empty, observable)
    }.uniqueHook("browse display-ad no-fill completion")
}

private object FollowingHeadlinerRequestFingerprint : Fingerprint(
    isStatic = false,
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf(HEADLINER_PREFERENCE, "ad_request_format_declined"),
)

internal fun BytecodePatchContext.blockBrowseDisplayAds() {
    val noFill = browseNoFill(FollowingHeadlinerRequestFingerprint.originalMethod)
    val request = FollowingHeadlinerRequestFingerprint.method
    val code = request.code()
    val (index, preference) = code.withIndex().filter { (_, instruction) ->
        instruction.opcode in setOf(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO) &&
            ((instruction as ReferenceInstruction).reference as StringReference).string == HEADLINER_PREFERENCE
    }.uniqueHook("browse display-ad preference read")
    val register = "v${(preference as OneRegisterInstruction).registerA}"
    // R8 may merge the request with unrelated callbacks. Guard only the headliner branch.
    request.addInstructionsWithLabels(index, """
        invoke-static {}, $AD_SETTINGS->displayAdsBlocked()Z
        move-result $register
        if-eqz $register, :original
        invoke-static {}, ${noFill.empty}
        move-result-object $register
        invoke-static/range {$register .. $register}, ${noFill.observable}
        move-result-object $register
        return-object $register
    """, ExternalLabel("original", preference))
}
