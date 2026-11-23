package dev.twitchpatches.patches.twitch.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import dev.twitchpatches.patches.twitch.shared.*

private object ChannelSkinModelFingerprint : Fingerprint(
    name = "toString",
    strings = listOf(string("ChannelSkin(campaignID=")),
)

private object ChannelSkinRenderContextFingerprint : Fingerprint(
    name = "toString",
    strings = listOf(string("ChannelSkinRenderContext(hasTurbo=")),
)

private object ShowingModelFingerprint : Fingerprint(
    name = "toString",
    strings = listOf(string("Showing(avatarURL=")),
)

private object ChannelSkinBannerFingerprint : Fingerprint(
    superclass = "Ltv/twitch/android/core/mvp/viewdelegate/BaseViewDelegate;",
    isStatic = false,
    returnType = "V",
    filters = listOf(methodCall("Landroid/view/ViewGroup;->addView(Landroid/view/View;)V")),
)

private object ChannelSkinHeaderFingerprint : Fingerprint(
    returnType = "V",
    isStatic = false,
    filters = listOf(methodCall(definingClass = "Ltv/twitch/android/feature/channelskins/ChannelSkinsCreatorBrandView;")),
)

private class ChannelSkinHiddenStateFingerprint(
    state: String,
) : Fingerprint(
    superclass = state,
    name = "<clinit>",
    returnType = "V",
)

internal data class ChannelSkinContract(val banner: Method, val hide: Method, val header: Method,
    val hidden: FieldReference?, val showingCheck: Int?)

internal fun BytecodePatchContext.channelSkinContract(): ChannelSkinContract {
    val skin = ChannelSkinModelFingerprint.originalClassDef.type
    val context = ChannelSkinRenderContextFingerprint.originalClassDef.type
    val showing = ShowingModelFingerprint.originalClassDef
    val state = requireNotNull(showing.superclass)

    val bannerMatches = ChannelSkinBannerFingerprint.matchAllOrNull()
        ?: throw PatchException("Channel sponsorship: no banner candidate matched.")
    val banner = bannerMatches.map { it.originalMethod }.single {
        it.parameterTypes.size == 5 &&
            it.parameterTypes.take(2).map(CharSequence::toString) == listOf(skin, context) &&
            it.parameterTypes.drop(2).distinct().size == 1
    }
    val hide = classDefBy(banner.definingClass).methods.filter {
        it.isInstance(emptyList(), "V") &&
            it.references().any { ref -> ref.toString() == "Landroid/view/ViewGroup;->removeView(Landroid/view/View;)V" }
    }.uniqueHook("native channel sponsorship removal")
    require(banner.references().none { it.toString() == hide.reference }) {
        "Channel sponsorship: rendering now clears the retained banner state."
    }

    val headerMatches = ChannelSkinHeaderFingerprint.matchAllOrNull()
        ?: throw PatchException("Channel sponsorship: no header candidate matched.")
    val header = headerMatches.map { it.originalMethod }.filter { method ->
        val channel = "Ltv/twitch/android/models/channel/ChannelModel;"
        (method.parameterTypes.size == 4 && method.parameterTypes[1] == channel && method.parameterTypes.last() == state) ||
            (method.isInstance(listOf("Ltv/twitch/android/core/mvp/viewdelegate/ViewDelegateState;"), "V") &&
                method.references().filterIsInstance<FieldReference>().any { it.type == state } &&
                method.references().filterIsInstance<FieldReference>().any { it.type == channel })
    }.uniqueHook("native channel sponsorship header")
    if (header.parameterTypes.size == 1) {
        val check = header.code().withIndex().filter { (_, instruction) ->
            instruction.opcode == Opcode.INSTANCE_OF &&
                ((instruction as ReferenceInstruction).reference as TypeReference).type == showing.type
        }.uniqueHook("native sponsorship header visibility check")
        return ChannelSkinContract(banner, hide, header, null, check.index)
    }

    val hiddenClass = ChannelSkinHiddenStateFingerprint(state).originalClassDef
    val hidden = hiddenClass.fields.filter { field ->
        field.type == field.definingClass &&
            AccessFlags.PUBLIC.isSet(field.accessFlags) &&
            AccessFlags.STATIC.isSet(field.accessFlags)
    }.uniqueHook("native hidden channel sponsorship state")

    return ChannelSkinContract(banner, hide, header, hidden, null)
}
