package dev.twitchpatches.patches.twitch.ui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal class MethodCallFingerprint(method: MethodReference) : Fingerprint(
    filters = listOf(methodCall(method)),
)

internal fun collectUiActionCallers(methods: Iterable<Method>, targets: Collection<MethodReference>): Map<String, List<Method>> {
    val owners = targets.mapTo(mutableSetOf()) { it.definingClass }
    val matches = targets.associate { it.toString() to mutableListOf<Method>() }
    for (method in methods) {
        val instructions = method.implementation?.instructions ?: continue
        for (instruction in instructions) {
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference ?: continue
            if (reference.definingClass !in owners) continue
            val callers = matches[reference.toString()] ?: continue
            if (callers.lastOrNull() != method) callers.add(method)
        }
    }
    return matches
}
