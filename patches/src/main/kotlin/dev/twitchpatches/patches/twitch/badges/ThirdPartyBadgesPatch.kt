package dev.twitchpatches.patches.twitch.badges

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import dev.twitchpatches.patches.twitch.emotes.CHANNEL_ID
import dev.twitchpatches.patches.twitch.emotes.inputMethod
import dev.twitchpatches.patches.twitch.settings.settingsPatch
import dev.twitchpatches.patches.twitch.shared.*
import dev.twitchpatches.patches.twitch.shared.hermes.HermesBundle
import dev.twitchpatches.patches.twitch.shared.hermes.MetroExports
import dev.twitchpatches.patches.twitch.shared.hermes.twitchBundle

private const val BADGES = "Ldev/twitchpatches/extension/badges/BadgeRuntime;"
private const val BADGE_BRIDGE = "Ldev/twitchpatches/extension/badges/BadgeBridge;"
private const val LIST = "Ljava/util/List;"
private const val STRING = "Ljava/lang/String;"
private const val OBJECT = "Ljava/lang/Object;"
private const val SPANNABLE_STRING = "Landroid/text/SpannableString;"
private val BRIDGE_FLAGS = AccessFlags.PUBLIC.value or AccessFlags.STATIC.value

private object MessageBadgeModelFingerprint : Fingerprint(
    name = "toString",
    strings = listOf(string("MessageBadgeViewModel(name=")),
)

private object ChatMessageUserFingerprint : Fingerprint(
    name = "toString",
    strings = listOf(string("ChatMessageUser(userId=")),
)

private class ChatRowBuilderFingerprint(badgeModel: String, chatUser: String) : Fingerprint(
    isStatic = true,
    custom = { method, _ ->
        val parameters = method.parameterTypes.map(CharSequence::toString)
        val senderParameter = parameters.indexOf(chatUser)
        senderParameter > 0 && parameters[senderParameter - 1] == LIST &&
            method.references().filterIsInstance<FieldReference>().any { it.definingClass == badgeModel }
    },
)

private fun requireBadgeImageContract(bundle: HermesBundle) {
    val selectors = bundle.functions.filter { bundle.strings[it.name] == "badgeImageForDensity" }
    require(selectors.isNotEmpty() && selectors.all { selector ->
        bundle.instructions(selector).filter { it.name.startsWith("GetById") }
            .mapNotNull { bundle.strings.getOrNull(it.args.last()) }.containsAll(setOf("image1x", "image2x", "image4x"))
    }) { "Badges: badge image selection changed." }
}

private val reactNativeBadgesPatch = resourcePatch {
    dependsOn(reactNativeBridgePatch)
    execute {
        val bundle = twitchBundle()
        val exports = MetroExports(bundle)
        val row =exports.resolveMemo("ChatMessageRow", "ChatMessageRowComponent", setOf("message", "badges", "badgeRegistry", "userId", "BadgePart"))
        val part = exports.resolve("BadgePart", setOf("badges", "registry", "loading"))
        exports.requireNestedContract("BadgePart", setOf("badgeKey", "chat-badge-"))
        exports.requireFunctionContract("BadgeImage", 2, setOf("badge", "set", "badgeImageForDensity"))
        requireBadgeImageContract(bundle)
        get(RN_ASSET).appendText("\n" + assetSource("badges.js")
            .replace("__TWITCH_BADGE_ROW_MODULE__", row.module.toString())
            .replace("__TWITCH_BADGE_PART_MODULE__", part.module.toString()))
    }
}

private fun BytecodePatchContext.generateBadgeBridge(badgeModel: ClassDef, chatUser: ClassDef) {
    val senderId = chatUser.fields.filter { it.type == CHANNEL_ID && AccessFlags.PUBLIC.isSet(it.accessFlags) }
        .uniqueHook("chat message sender id")
    val modelConstructor = badgeModel.methods.filter {
        it.name == "<init>" && it.parameterTypes.map(CharSequence::toString) == listOf(STRING, STRING)
    }.uniqueHook("message badge constructor")
    val bridge = mutableClassDefBy(BADGE_BRIDGE)
    bridge.methods.removeIf { it.name in setOf("senderId", "model") }
    bridge.methods.add(inputMethod(BADGE_BRIDGE, "senderId", listOf(OBJECT), STRING, 3, BRIDGE_FLAGS, """
        check-cast p0, ${chatUser.type}
        iget-object v0, p0, ${chatUser.type}->${senderId.name}:$CHANNEL_ID
        if-nez v0, :known
        const/4 v1, 0x0
        return-object v1
        :known
        invoke-virtual {v0}, $CHANNEL_ID->toString()$STRING
        move-result-object v1
        return-object v1
    """))
    bridge.methods.add(inputMethod(BADGE_BRIDGE, "model", listOf(STRING, STRING), OBJECT, 3, BRIDGE_FLAGS, """
        new-instance v0, ${badgeModel.type}
        invoke-direct {v0, p0, p1}, ${modelConstructor.reference}
        return-object v0
    """))
}

private fun BytecodePatchContext.addBadgeToChatRows(rowBuilder: Method, chatUser: String) {
    val owner = mutableClassDefBy(rowBuilder.definingClass)
    val builder = owner.methods.single { it.reference == rowBuilder.reference }
    val senderParameter = builder.parameterTypes.map(CharSequence::toString).indexOf(chatUser)
    val badgesParameter = senderParameter - 1
    val badgeText = builder.code().filter { it.opcode == Opcode.INVOKE_STATIC || it.opcode == Opcode.INVOKE_STATIC_RANGE }
        .mapNotNull { it.methodReference() }.filter {
            it.definingClass == owner.type && it.returnType == SPANNABLE_STRING &&
                it.parameterTypes.map(CharSequence::toString).count { type -> type == STRING } == 2
        }.distinctBy { it.toString() }.uniqueHook("chat badge text builder")
    builder.addInstructions(0, """
        invoke-static/range {p$badgesParameter .. p$senderParameter}, $BADGES->withRowBadge($LIST$OBJECT)$LIST
        move-result-object p$badgesParameter
    """)
    val textBuilder = owner.methods.single { it.reference == badgeText.toString() }
    val urlParameter = textBuilder.parameterTypes.map(CharSequence::toString).lastIndexOf(STRING)
    textBuilder.addInstructionsWithLabels(0, """
        invoke-static/range {p$urlParameter .. p$urlParameter}, $BADGES->rowBadgeText($STRING)$SPANNABLE_STRING
        move-result-object v0
        if-eqz v0, :original
        return-object v0
        :original
        nop
    """)
}

private fun Instruction.methodReference() = (this as? ReferenceInstruction)?.reference as? MethodReference

@Suppress("unused")
val thirdPartyBadgesPatch = bytecodePatch(
    name = "7TV, FFZ, Chatterino and DankChat badges",
    description = "Shows one third-party badge next to the Twitch badges in live and replay chat.",
    default = true,
) {
    compatibleWith(TwitchTarget.compatibility)
    dependsOn(settingsPatch, reactNativeBadgesPatch)
    execute {
        val badgeModel = MessageBadgeModelFingerprint.originalClassDef
        val chatUser = ChatMessageUserFingerprint.originalClassDef
        generateBadgeBridge(badgeModel, chatUser)
        addBadgeToChatRows(ChatRowBuilderFingerprint(badgeModel.type, chatUser.type).originalMethod, chatUser.type)
        val application = mutableClassDefBy("Ltv/twitch/android/app/consumer/TwitchApplication;").methods
            .filter { it.name == "onCreate" && it.isInstance(emptyList(), "V") }
            .uniqueHook("badge initialization")
        application.code().withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }.asReversed()
            .forEach { application.insertAtReturn(it, "invoke-static/range {p0 .. p0}, $BADGES->initialize(Landroid/app/Application;)V") }
    }
}
