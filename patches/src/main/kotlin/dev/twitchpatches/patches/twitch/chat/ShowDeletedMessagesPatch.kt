package dev.twitchpatches.patches.twitch.chat

import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import dev.twitchpatches.patches.twitch.settings.settingsPatch
import dev.twitchpatches.patches.twitch.shared.*

// Adapted from arandomhooman/hoomans-morphe-patches (GPL-3.0), Show deleted messages.
internal const val DELETED_POLICY = "Ldev/twitchpatches/extension/chat/DeletedMessageSettings;"
internal const val CHAT_DISPATCHER = "Ltv/twitch/android/core/mvp/viewdelegate/EventDispatcher;"

@Suppress("unused")
val showDeletedMessagesPatch = bytecodePatch(
    name = "Show deleted messages",
    description = "Keeps deleted and moderated chat messages readable.",
    default = true,
) {
    compatibleWith(TwitchTarget.compatibility)
    dependsOn(settingsPatch, reactNativeDeletedMessagesPatch)
    execute {
        val formatters = mutableListOf<Method>()
        classDefForEach { type -> formatters.addAll(type.methods.filter {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.returnType == "Landroid/text/Spanned;" &&
                it.parameterTypes.map { p -> p.toString() } == listOf("Ljava/lang/String;",
                    "Landroid/text/SpannedString;", "Landroid/content/Context;", CHAT_DISPATCHER, "Z")
        }) }
        val formatterHook = formatters.uniqueHook("deleted message formatter")
        keepDeletedMessageText(mutableClassDefBy(formatterHook.definingClass).methods.single { it.reference == formatterHook.reference })
        val application = mutableClassDefBy("Ltv/twitch/android/app/consumer/TwitchApplication;").methods
            .filter { it.name == "onCreate" && it.isInstance(emptyList(), "V") }
            .uniqueHook("deleted message settings initialization")
        application.code().withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }.asReversed()
            .forEach { application.insertAtReturn(it, "invoke-static/range {p0 .. p0}, $DELETED_POLICY->initialize(Landroid/app/Application;)V") }
    }
}
