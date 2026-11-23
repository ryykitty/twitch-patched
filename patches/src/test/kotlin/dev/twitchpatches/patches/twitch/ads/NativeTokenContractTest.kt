package dev.twitchpatches.patches.twitch.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import dev.twitchpatches.patches.twitch.shared.code
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeTokenContractTest {
    private fun observerMethod(name: String, parameter: String) = ImmutableMethod("Lsynthetic/Observer;", name,
        listOf(ImmutableMethodParameter(parameter, null, null)), "V", AccessFlags.PUBLIC.value or AccessFlags.ABSTRACT.value,
        null, null, null)

    @Test fun observerCallbacksResolveAfterIndependentMethodRenaming() {
        val callbacks = resolveTokenObserver(listOf(observerMethod("success", "Ljava/lang/Object;"),
            observerMethod("failure", "Ljava/lang/Throwable;"), observerMethod("cancel", "Lsynthetic/Disposable;")))
        assertEquals("success", callbacks.success.name)
        assertEquals("failure", callbacks.error.name)
        assertEquals("cancel", callbacks.subscribed.name)
    }

    @Test(expected = PatchException::class) fun ambiguousObserverErrorsAreRejected() {
        resolveTokenObserver(listOf(observerMethod("success", "Ljava/lang/Object;"),
            observerMethod("failure", "Ljava/lang/Throwable;"), observerMethod("other", "Ljava/lang/Throwable;"),
            observerMethod("cancel", "Lsynthetic/Disposable;")))
    }

    private fun method(body: String) = ImmutableMethod("Lsynthetic/Token;", "request",
        listOf(ImmutableMethodParameter("J", null, null), ImmutableMethodParameter("Z", null, null)), "V",
        AccessFlags.PUBLIC.value, null, null, MutableMethodImplementation(6)).toMutable().apply {
            addInstructionsWithLabels(0, body)
        }

    @Test fun parameterWordIncludesReceiverAndWideArguments() { assertEquals(3, parameterWord(method("return-void"), 1)) }

    @Test fun literalResolutionUsesTheLastRealRegisterWrite() {
        val method = method("""
            const/16 v0, 335
            const/16 v0, 24
            invoke-static/range {v0 .. v0}, Lsynthetic/Query;->flags(I)V
            return-void
        """)
        assertEquals(24, method.literalBefore(2, 0))
        assertEquals(listOf(0), invokeRegisters(method.code()[2]))
    }

    private val mapperType = "Lsynthetic/Mapper;"
    private val parserType = "Lsynthetic/Parser;"
    private val mapperConstructor = ImmutableMethodReference(mapperType, "<init>", listOf("Ljava/lang/Object;", "I"), "V")

    private fun mapperMethod(name: String, parameters: List<String>, returns: String, registers: Int, body: String) =
        ImmutableMethod(mapperType, name, parameters.map { ImmutableMethodParameter(it, null, null) }, returns,
            AccessFlags.PUBLIC.value, null, null, MutableMethodImplementation(registers)).toMutable().apply {
                addInstructionsWithLabels(0, body)
            }

    // Mirrors Twitch 32.1 ylo: the stream parser is the sparse-switch default, VOD is key 0x1.
    private fun boundMapper() = ImmutableClassDef(mapperType, AccessFlags.PUBLIC.value, "Ljava/lang/Object;",
        listOf("Lkotlin/jvm/functions/Function1;"), null, null, null, listOf(
            mapperMethod("<init>", listOf("Ljava/lang/Object;", "I"), "V", 6, """
                iput p2, p0, $mapperType->a:I
                sparse-switch p2, :data
                const-string v0, "$TOKEN_PARSER_SIGNATURE"
                const-class v1, $parserType
                const-string v2, "$TOKEN_PARSER"
                return-void
                :vod
                const-string v0, "parseVodAccessTokenResponse(Ltv/twitch/gql/VodAccessTokenQuery${'$'}Data;)Ltv/twitch/android/models/AccessTokenResponse;"
                const-class v1, Lsynthetic/OtherParser;
                const-string v2, "parseVodAccessTokenResponse"
                return-void
                :data
                .sparse-switch
                    0x1 -> :vod
                .end sparse-switch
            """),
            mapperMethod("invoke", listOf("Ljava/lang/Object;"), "Ljava/lang/Object;", 4, """
                iget v0, p0, $mapperType->a:I
                packed-switch v0, :data
                check-cast p1, Lsynthetic/VodData;
                return-object p1
                :stream
                iget-object v1, p0, $mapperType->receiver:Ljava/lang/Object;
                check-cast v1, $parserType
                invoke-static {p1}, $parserType->a(Ljava/lang/Object;)Ltv/twitch/android/models/AccessTokenResponse;
                move-result-object v0
                return-object v0
                :data
                .packed-switch 0x0
                    :stream
                .end packed-switch
            """),
        ))

    @Test fun boundMapperDiscriminatorMustSelectTheStreamParser() {
        verifyBoundTokenMapper(boundMapper(), mapperConstructor, 0, parserType)
    }

    @Test(expected = PatchException::class) fun boundMapperRejectsTheVodDiscriminator() {
        verifyBoundTokenMapper(boundMapper(), mapperConstructor, 1, parserType)
    }

    @Test(expected = PatchException::class) fun boundMapperRejectsAForeignParserOwner() {
        verifyBoundTokenMapper(boundMapper(), mapperConstructor, 0, "Lsynthetic/OtherParser;")
    }

    @Test fun boundMapperBridgeRebuildsTheAppConstructor() {
        val parser = ImmutableFieldReference("Lsynthetic/Service;", "b", parserType)
        assertEquals("""
            new-instance v3, $mapperType
            iget-object v4, v0, Lsynthetic/Service;->b:$parserType
            const v5, 0
            invoke-direct {v3, v4, v5}, $mapperType-><init>(Ljava/lang/Object;I)V
        """.trimIndent(), tokenMapper(BoundTokenMapper(mapperConstructor, 0), parser))
    }

    @Test(expected = PatchException::class) fun overwrittenLiteralCannotBeReused() {
        method("""
            const/16 v0, 335
            move-object v0, p0
            invoke-static/range {v0 .. v0}, Lsynthetic/Query;->value(Ljava/lang/Object;)V
            return-void
        """).literalBefore(2, 0)
    }
}
