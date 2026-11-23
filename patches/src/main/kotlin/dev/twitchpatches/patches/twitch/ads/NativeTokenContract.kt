package dev.twitchpatches.patches.twitch.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import dev.twitchpatches.patches.twitch.shared.*

private object SingleSubscriptionFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(string("The RxJavaPlugins.onSubscribe hook returned a null SingleObserver.", StringComparisonType.STARTS_WITH)),
)

private class TokenSerializerFingerprint(playerField: FieldReference) : Fingerprint(
    strings = listOf("platform", "playerType", "android"),
    filters = listOf(fieldAccess(playerField))
)

internal data class NativeTokenContract(
    val service: ClassDef, val constructor: Method, val params: MethodReference, val absent: FieldReference,
    val paramsMask: Int, val query: MethodReference, val mapper: NativeTokenMapper, val parser: FieldReference,
    val graphql: MethodReference, val graphqlField: FieldReference,
    val queryFlags: Int, val subscribe: Method, val observer: NativeTokenObserver, val dispose: Method,
    val result: FieldReference, val serializer: Method, val platformIndex: Int,
    val platformRegister: Int, val paramsRegister: Int,
)

private const val STRING = "Ljava/lang/String;"
private const val OBJECT = "Ljava/lang/Object;"
private const val FUNCTION1 = "Lkotlin/jvm/functions/Function1;"
internal fun Method.callRefs() = code().withIndex().mapNotNull { (index, instruction) ->
    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let { Triple(index, instruction, it) }
}

private fun isGraphqlSingle(instruction: Instruction, ref: MethodReference) = instruction.opcode == Opcode.INVOKE_STATIC &&
    ref.parameterTypes.size == 4 && ref.parameterTypes[2].toString() == FUNCTION1 && ref.parameterTypes[3].toString() == "I"

private object StreamTokenQueryFingerprint : Fingerprint(
    name = "name",
    returnType = STRING,
    strings = listOf("StreamAccessTokenQuery"),
)

private class StreamTokenServiceFingerprint(query: Method) : Fingerprint(
    isStatic = false,
    returnType = OBJECT,
    strings = listOf("call to 'resume' before 'invoke' with coroutine"),
    filters = listOf(methodCall(query.reference)),
    custom = { method, _ -> method.parameterTypes.size == 3 && method.parameterTypes.first() == STRING },
)

internal fun BytecodePatchContext.resolveNativeTokens(): NativeTokenContract {
    val query = StreamTokenQueryFingerprint.originalClassDef.methods.filter {
        it.name == "<init>" && it.parameterTypes.size == 2 && it.parameterTypes.first() == STRING
    }.uniqueHook("native stream token query")
    val source = StreamTokenServiceFingerprint(query).originalMethod
    val service = classDefBy(source.definingClass)
    val code = source.code()
    val calls = source.callRefs()
    val paramsCall = calls.filter { (_, instruction, ref) -> ref.name == "<init>" &&
        ref.parameterTypes.size == 3 && ref.parameterTypes[1].toString() == STRING &&
        ref.parameterTypes[2].toString() == "I" && instruction.opcode == Opcode.INVOKE_DIRECT &&
        classDefBy(ref.definingClass).methods.any { method -> method.references().filterIsInstance<StringReference>()
            .any { it.string.startsWith("PlaybackAccessTokenParams(device=") } }
    }.uniqueHook("native playback token parameters")
    val params = paramsCall.third
    val paramsRegisters = invokeRegisters(paramsCall.second)
    val mask = source.literalBefore(paramsCall.first, paramsRegisters.last())
    val absent = code.take(paramsCall.first).filter { it.opcode == Opcode.SGET_OBJECT }
        .mapNotNull { (it as? ReferenceInstruction)?.reference as? FieldReference }
        .filter { classDefBy(it.type).superclass == params.parameterTypes.first().toString() }
        .uniqueHook("native absent token option")
    require(query.parameterTypes[1] == params.definingClass) { "Native stream token query parameters changed." }
    val graphqlCall = calls.filter { (_, instruction, ref) -> isGraphqlSingle(instruction, ref) }
        .uniqueHook("native authorized GraphQL Single")
    val graphRegisters = invokeRegisters(graphqlCall.second)
    val (mapper, parser) = resolveTokenMapper(source, graphqlCall.first, graphRegisters[2])
    val graphqlField = source.fieldBefore(graphqlCall.first, graphRegisters[0])
    if (graphqlField.type != graphqlCall.third.parameterTypes[0].toString() ||
        graphqlField.definingClass != service.type || parser.definingClass != service.type)
        throw PatchException("Native token service fields do not match GraphQL/mapper ownership")
    val constructor = service.methods.filter { it.name == "<init>" &&
        it.parameterTypes.map { p -> p.toString() } == listOf(graphqlField.type, parser.type) &&
        AccessFlags.PUBLIC.isSet(it.accessFlags)
    }.uniqueHook("native token source lifetime")
    val queryFlags = source.literalBefore(graphqlCall.first, graphRegisters[3])
    val subscribe = SingleSubscriptionFingerprint.originalMethod
    var parent: String? = graphqlCall.third.returnType
    val visited = mutableSetOf<String>()
    while (parent != null && parent != subscribe.definingClass && visited.add(parent)) parent = classDefBy(parent).superclass
    if (parent != subscribe.definingClass) throw PatchException("Token GraphQL result is not the resolved Single type")
    val observer = classDefBy(subscribe.parameterTypes.single().toString())
    if (!AccessFlags.INTERFACE.isSet(observer.accessFlags)) throw PatchException("Token observer is not an interface")
    val callbacks = resolveTokenObserver(observer.methods)
    val dispose = classDefBy(callbacks.subscribed.parameterTypes.single().toString()).methods.filter {
        it.name == "dispose" && it.parameterTypes.isEmpty() && it.returnType == "V"
    }.uniqueHook("native token disposal")
    val result = code.filter { it.opcode == Opcode.IGET_OBJECT }.mapNotNull {
        (it as? ReferenceInstruction)?.reference as? FieldReference
    }.lastOrNull { it.type == OBJECT &&
        "Ljava/io/Serializable;" in classDefBy(it.definingClass).interfaces }
        ?: throw PatchException("Native token result wrapper not found")
    val playerField = classDefBy(params.definingClass).fields.filter { it.type == STRING &&
        !AccessFlags.STATIC.isSet(it.accessFlags) }.uniqueHook("token player-type field")
    val serializer = TokenSerializerFingerprint(playerField).originalMethod
    val serialCode = serializer.code()
    val cast = serialCode.withIndex().filter { (_, instruction) -> instruction.opcode == Opcode.CHECK_CAST &&
        ((instruction as? ReferenceInstruction)?.reference as? TypeReference)?.type == params.definingClass }
        .uniqueHook("serializer token-parameter cast")
    val android = serialCode.withIndex().filter { (index, instruction) -> index > cast.index &&
        ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string == "android" }
        .uniqueHook("token-specific platform literal")
    if (serialCode.getOrNull(android.index + 1)?.opcode != Opcode.INVOKE_VIRTUAL)
        throw PatchException("Token platform serialization does not immediately consume its literal")
    return NativeTokenContract(service, constructor, params, absent, mask, query, mapper, parser,
        graphqlCall.third, graphqlField, queryFlags, subscribe, callbacks, dispose, result,
        serializer, android.index, (android.value as OneRegisterInstruction).registerA,
        (cast.value as OneRegisterInstruction).registerA)
}

internal data class NativeTokenObserver(val success: Method, val error: Method, val subscribed: Method)

internal fun resolveTokenObserver(methods: Iterable<Method>): NativeTokenObserver {
    val callbacks = methods.filter { it.returnType == "V" && it.parameterTypes.size == 1 &&
        AccessFlags.PUBLIC.isSet(it.accessFlags) && AccessFlags.ABSTRACT.isSet(it.accessFlags) }
    val success = callbacks.filter { it.parameterTypes.single().toString() == OBJECT }
        .uniqueHook("native token observer success")
    val error = callbacks.filter { it.parameterTypes.single().toString() == "Ljava/lang/Throwable;" }
        .uniqueHook("native token observer error")
    val subscribed = callbacks.filter { it != success && it != error }.uniqueHook("native token subscription cancellation")
    return NativeTokenObserver(success, error, subscribed)
}

internal fun invokeRegisters(instruction: Instruction): List<Int> = when (instruction) {
    is RegisterRangeInstruction -> (instruction.startRegister until instruction.startRegister + instruction.registerCount).toList()
    is FiveRegisterInstruction -> listOf(instruction.registerC, instruction.registerD, instruction.registerE,
        instruction.registerF, instruction.registerG).take(instruction.registerCount)
    else -> throw PatchException("Token invocation has no supported register shape")
}

internal fun Method.literalBefore(index: Int, register: Int): Int {
    val write = code().take(index).asReversed().firstOrNull { writesRegister(it, register) }
    return (write as? NarrowLiteralInstruction)?.narrowLiteral
        ?: throw PatchException("Token constructor literal was overwritten or unresolved for v$register")
}

internal fun Method.fieldBefore(index: Int, register: Int): FieldReference {
    val write = code().take(index).asReversed().firstOrNull { writesRegister(it, register) }
    return ((write as? ReferenceInstruction)?.reference as? FieldReference)
        ?.takeIf { write.opcode == Opcode.IGET_OBJECT }
        ?: throw PatchException("Token service field was overwritten or unresolved for v$register")
}

internal fun writesRegister(instruction: Instruction, register: Int): Boolean {
    return (instruction as? OneRegisterInstruction)?.registerA == register && instruction.opcode.setsRegister()
}
