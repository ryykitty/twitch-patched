package dev.twitchpatches.patches.twitch.ads

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import dev.twitchpatches.patches.twitch.shared.*

private const val STRING = "Ljava/lang/String;"
private const val OBJECT = "Ljava/lang/Object;"
private const val FUNCTION1 = "Lkotlin/jvm/functions/Function1;"
private const val TOKEN_RESPONSE = "Ltv/twitch/android/models/AccessTokenResponse;"
internal const val TOKEN_PARSER = "parseStreamAccessTokenResponse"
internal const val TOKEN_PARSER_SIGNATURE =
    "parseStreamAccessTokenResponse(Ltv/twitch/gql/StreamAccessTokenQuery\$Data;)$TOKEN_RESPONSE"
private val REFLECTIVE_MAPPER = listOf("I", OBJECT, "Ljava/lang/Class;", STRING, STRING, "I", "I")
private val BOUND_MAPPER = listOf(OBJECT, "I")

/** How the app builds the Function1 that maps StreamAccessTokenQuery data to an AccessTokenResponse. */
internal sealed interface NativeTokenMapper { val constructor: MethodReference }

internal data class ReflectiveTokenMapper(override val constructor: MethodReference, val arguments: List<Int>) : NativeTokenMapper

internal data class BoundTokenMapper(override val constructor: MethodReference, val discriminator: Int) : NativeTokenMapper

/** Traces the GraphQL Function1 argument back to its allocation and proves it maps through the service's parser. */
internal fun BytecodePatchContext.resolveTokenMapper(
    source: Method, graphqlIndex: Int, register: Int,
): Pair<NativeTokenMapper, FieldReference> {
    val code = source.code()
    val (index, instruction, ref) = source.callRefs().lastOrNull { (index, instruction, ref) -> index < graphqlIndex &&
        ref.name == "<init>" && instruction.opcode in setOf(Opcode.INVOKE_DIRECT, Opcode.INVOKE_DIRECT_RANGE) &&
        invokeRegisters(instruction).first() == register
    } ?: throw PatchException("Native token mapper construction not found")
    if (code.subList(index + 1, graphqlIndex).any { writesRegister(it, register) })
        throw PatchException("Native token mapper register is overwritten before GraphQL")
    val allocation = code.take(index).asReversed().firstOrNull { writesRegister(it, register) }
    if (allocation?.opcode != Opcode.NEW_INSTANCE ||
        ((allocation as ReferenceInstruction).reference as TypeReference).type != ref.definingClass)
        throw PatchException("Native token mapper is not freshly allocated")
    val registers = invokeRegisters(instruction)
    return when (ref.parameterTypes.map { it.toString() }) {
        REFLECTIVE_MAPPER -> {
            if (registers.size != 8) throw PatchException("Native token mapper must use eight argument words")
            val parser = source.fieldBefore(index, registers[2])
            if (source.typeBefore(index, registers[3]) != parser.type ||
                source.stringBefore(index, registers[4]) != TOKEN_PARSER ||
                source.stringBefore(index, registers[5]) != TOKEN_PARSER_SIGNATURE)
                throw PatchException("Native token mapper does not reference the stream token parser")
            ReflectiveTokenMapper(ref, listOf(1, 6, 7).map { source.literalBefore(index, registers[it]) }) to parser
        }
        BOUND_MAPPER -> {
            if (registers.size != 3) throw PatchException("Native token mapper must use three argument words")
            val parser = source.fieldBefore(index, registers[1])
            val discriminator = source.literalBefore(index, registers[2])
            verifyBoundTokenMapper(classDefBy(ref.definingClass), ref, discriminator, parser.type)
            BoundTokenMapper(ref, discriminator) to parser
        }
        else -> throw PatchException("Native token mapper constructor shape changed: $ref")
    }
}

/** Proves that the discriminator selects the stream token parser in both the constructor metadata and invoke(). */
internal fun verifyBoundTokenMapper(mapper: ClassDef, ref: MethodReference, discriminator: Int, parserType: String) {
    if (FUNCTION1 !in mapper.interfaces) throw PatchException("Native token mapper is not a Function1")
    val constructor = mapper.methods.filter { it.reference == ref.toString() && it.implementation != null }
        .uniqueHook("native token mapper constructor")
    val keyRegister = requireNotNull(constructor.implementation).registerCount - 1
    val ctorCode = constructor.code()
    val store = ctorCode.withIndex().filter { (_, insn) -> insn.opcode == Opcode.IPUT &&
        (insn as TwoRegisterInstruction).registerA == keyRegister }
        .filter { (_, insn) -> ((insn as ReferenceInstruction).reference as FieldReference).let { field ->
            field.definingClass == mapper.type && field.type == "I" } }
        .uniqueHook("native token mapper discriminator store")
    val field = (store.value as ReferenceInstruction).reference as FieldReference
    val ctorRefs = constructor.branchReferences(constructor.switchTarget(keyRegister, discriminator, store.index))
    if (ctorRefs.filterIsInstance<StringReference>().map { it.string }.let {
            TOKEN_PARSER !in it || TOKEN_PARSER_SIGNATURE !in it } ||
        ctorRefs.filterIsInstance<TypeReference>().none { it.type == parserType })
        throw PatchException("Native token mapper discriminator $discriminator is not the stream token parser")
    val invoke = mapper.methods.filter { it.name == "invoke" && it.implementation != null &&
        it.parameterTypes.map { p -> p.toString() } == listOf(OBJECT) && it.returnType == OBJECT
    }.uniqueHook("native token mapper invoke")
    val read = invoke.code().withIndex().filter { (_, insn) -> insn.opcode == Opcode.IGET &&
        ((insn as ReferenceInstruction).reference as FieldReference).let { f ->
            f.definingClass == field.definingClass && f.name == field.name && f.type == field.type } }
        .uniqueHook("native token mapper discriminator read")
    val invokeRefs = invoke.branchReferences(
        invoke.switchTarget((read.value as TwoRegisterInstruction).registerA, discriminator, read.index))
    if (invokeRefs.filterIsInstance<TypeReference>().none { it.type == parserType } ||
        invokeRefs.filterIsInstance<MethodReference>().none {
            it.definingClass == parserType && it.returnType == TOKEN_RESPONSE })
        throw PatchException("Native token mapper invoke branch does not produce a stream access token")
}

/** Returns the instruction index selected by the first switch on [register] after [from] for [key]. */
private fun Method.switchTarget(register: Int, key: Int, from: Int): Int {
    val code = code()
    val switch = code.indices.firstOrNull { it > from &&
        code[it].opcode in setOf(Opcode.PACKED_SWITCH, Opcode.SPARSE_SWITCH) &&
        (code[it] as OneRegisterInstruction).registerA == register
    } ?: throw PatchException("Native token mapper dispatch switch not found")
    if (code.subList(from + 1, switch).any { writesRegister(it, register) })
        throw PatchException("Native token mapper discriminator is overwritten before dispatch")
    val addresses = code.runningFold(0) { address, instruction -> address + instruction.codeUnits }
    fun indexAt(address: Int) = addresses.indexOf(address).takeIf { it in code.indices }
        ?: throw PatchException("Native token mapper switch target is unresolved")
    val payload = code[indexAt(addresses[switch] + (code[switch] as OffsetInstruction).codeOffset)] as? SwitchPayload
        ?: throw PatchException("Native token mapper switch payload is unresolved")
    return payload.switchElements.firstOrNull { it.key == key }?.let { indexAt(addresses[switch] + it.offset) } ?: (switch + 1)
}

/** References on the straight-line branch from [start] up to its first return. */
private fun Method.branchReferences(start: Int): List<Reference> {
    val branch = mutableListOf<Reference>()
    for (instruction in code().drop(start)) {
        (instruction as? ReferenceInstruction)?.reference?.let(branch::add)
        if (instruction.opcode in RETURNS) return branch
        if (instruction.opcode in DISPATCH) break
    }
    throw PatchException("Native token mapper branch does not reach a return")
}

private val RETURNS = setOf(Opcode.RETURN_VOID, Opcode.RETURN, Opcode.RETURN_OBJECT, Opcode.RETURN_WIDE, Opcode.THROW)
private val DISPATCH = setOf(Opcode.PACKED_SWITCH, Opcode.SPARSE_SWITCH,
    Opcode.PACKED_SWITCH_PAYLOAD, Opcode.SPARSE_SWITCH_PAYLOAD)

private fun Method.stringBefore(index: Int, register: Int): String? {
    val write = code().take(index).asReversed().firstOrNull { writesRegister(it, register) }
    return ((write as? ReferenceInstruction)?.reference as? StringReference)?.string
}

private fun Method.typeBefore(index: Int, register: Int): String? {
    val write = code().take(index).asReversed().firstOrNull { writesRegister(it, register) }
    return ((write as? ReferenceInstruction)?.reference as? TypeReference)?.type
        ?.takeIf { write.opcode == Opcode.CONST_CLASS }
}
