package dev.twitchpatches.patches.twitch.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.literal
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import dev.twitchpatches.patches.twitch.reload.nativeReloadMethod
import dev.twitchpatches.patches.twitch.shared.*

private class ViewLayoutSelectorFingerprint(splitId: Long) : Fingerprint(
    returnType = "V",
    filters = listOf(literal(splitId)),
)

internal fun BytecodePatchContext.restoreVerticalLayoutControls(contract: UiCompositionContract) {
    val selector = ViewLayoutSelectorFingerprint(uiResourceIds.getValue("string/view_mode_classic_split")).matchAll()
        .map { it.originalMethod }
        .filter { method ->
            method.returnType == "V" && method.parameterTypes.size == 7 &&
                method.parameterTypes[4].toString() == contract.composer && method.references().any {
                    it is FieldReference && it.name == "VerticalView"
                }
        }.uniqueHook("native view layout selector")
    val vertical = classDefBy(selector.parameterTypes[1].toString()).fields.filter { it.name == "VerticalView" &&
        it.type == selector.parameterTypes[1] }.uniqueHook("native Vertical View identity")
    val target = contract.vertical
    val callback = selector.parameterTypes[2]
    require(target.parameterTypes[10] == callback && target.parameterTypes[24] == contract.composer &&
        target.parameterTypes.none { it == "J" || it == "D" }) { "UI settings: vertical layout callback contract changed." }
    val code = target.code()
    val volumeIndex = code.withIndex().filter { (_, instruction) ->
        val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        ref?.parameterTypes == listOf("Z", callback, contract.composer, "I") && ref.returnType == "V"
    }.uniqueHook("vertical volume position").index
    val supportIndex = code.take(volumeIndex).withIndex().filter { (_, instruction) ->
        val call = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        call?.definingClass == target.definingClass && call.parameterTypes == listOf("I", contract.composer, callback, callback, "Z")
    }.uniqueHook("vertical support controls").index
    val slot = code.withIndex().filter { (index, instruction) ->
        val call = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        val close = code.getOrNull(index + 1) as? FiveRegisterInstruction
        index in (supportIndex + 1) until volumeIndex && call?.toString() == contract.start.toString() &&
            (close as? ReferenceInstruction)?.reference?.toString() == contract.end.toString() &&
            close.registerC == (instruction as FiveRegisterInstruction).registerC
    }.uniqueHook("empty vertical layout control slot")
    val parameters = requireNotNull(target.implementation).registerCount - target.parameterTypes.size
    require(code.take(slot.index).none { it.opcode.setsRegister() && it is OneRegisterInstruction &&
        it.registerA == parameters + 10 }) { "UI settings: vertical layout callback is overwritten." }
    val group = slot.value as FiveRegisterInstruction
    val key = code[slot.index - 1] as? OneRegisterInstruction
        ?: error("UI settings: vertical control group key changed.")
    require(key.opcode == Opcode.CONST && group.registerCount == 2 &&
        key.registerA == group.registerD && key.registerA != group.registerC) {
        "UI settings: vertical control group key changed."
    }
    val owner = mutableClassDefBy("Ldev/twitchpatches/extension/ui/UiComposeBridge;")
    val render = nativeReloadMethod(owner.type, "verticalLayoutControls", listOf(callback.toString(), contract.composer), "V", 9, """
        const/4 v0, 0x0
        sget-object v1, $vertical
        move-object v2, p0
        const/4 v3, 0x0
        move-object v4, p1
        const/16 v5, 0x30
        const/16 v6, 0x9
        invoke-static/range {v0 .. v6}, ${selector.reference}
        return-void
    """, static = true)
    owner.methods.add(render)
    mutableClassDefBy(target.definingClass).methods.single { it.reference == target.reference }
        .addInstructions(slot.index + 1, """
            move-object/from16 v${key.registerA}, p10
            invoke-static {v${key.registerA}, v${group.registerC}}, ${render.reference}
            const v${key.registerA}, ${(key as NarrowLiteralInstruction).narrowLiteral}
        """)
}
