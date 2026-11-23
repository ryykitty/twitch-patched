package dev.twitchpatches.patches.twitch.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseDisplayAdsTest {
    private fun completion(argument: String = "v0", returned: String = "v0", twice: Boolean = false,
                           sharedReturn: Boolean = false) =
        ImmutableMethod("Lsynthetic/Request;", "request", emptyList(), "Ljava/lang/Object;",
            AccessFlags.PUBLIC.value, null, null, MutableMethodImplementation(4)).toMutable().apply {
            val body = """
                invoke-static {}, Ltv/twitch/android/util/Optional${'$'}Companion;->empty()Ltv/twitch/android/util/Optional;
                move-result-object v0
                invoke-static {$argument}, Lio/reactivex/u;->just(Ljava/lang/Object;)Lio/reactivex/internal/operators/observable/Just;
                move-result-object v0
                ${if (sharedReturn) "goto :return\nconst/4 v0, 0x0\n:return" else ""}
                return-object $returned
            """
            addInstructionsWithLabels(0, body + if (twice) body else "")
        }

    @Test fun derivesNativeEmptyOptionalAndReactiveFactory() {
        val completion = browseNoFill(completion())
        assertEquals("empty", completion.empty.name)
        assertEquals("just", completion.observable.name)
    }

    @Test fun derivesCompletionThroughSharedReturn() {
        assertEquals("just", browseNoFill(completion(sharedReturn = true)).observable.name)
    }

    @Test(expected = PatchException::class) fun rejectsChangedSharedReturnFlow() {
        browseNoFill(completion(returned = "v1", sharedReturn = true))
    }

    @Test(expected = PatchException::class) fun rejectsUnrelatedReactiveArgument() {
        browseNoFill(completion(argument = "v1"))
    }

    @Test(expected = PatchException::class) fun rejectsChangedReturnFlow() {
        browseNoFill(completion(returned = "v1"))
    }

    @Test(expected = PatchException::class) fun rejectsAmbiguousCompletion() {
        browseNoFill(completion(twice = true))
    }
}
