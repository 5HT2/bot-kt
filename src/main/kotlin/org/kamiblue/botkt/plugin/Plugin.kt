package org.kamiblue.botkt.plugin

import org.kamiblue.botkt.BackgroundJob
import org.kamiblue.botkt.BackgroundScope
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.event.BotEventBus
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.commons.collections.CloseableList
import org.kamiblue.commons.interfaces.Nameable
import org.kamiblue.event.ListenerManager

/**
 * A plugin. All plugin main classes must extend this class.
 *
 * The methods onLoad and onUnload may be implemented by your
 * plugin in order to do stuff when the plugin is loaded and
 * unloaded, respectively.
 */
open class Plugin : Nameable {

    private lateinit var info: PluginInfo
    override val name: String get() = info.name
    val version: String get() = info.version
    val botVersion: String get() = info.botVersion
    val description: String get() = info.description
    val authors: Array<String> get() = info.authors
    val requiredPlugins: Array<String> get() = info.requiredPlugins
    val url: String get() = info.url
    val hotReload: Boolean get() = info.hotReload

    /**
     * The list of [Manager] the plugin will add.
     *
     * @sample org.kamiblue.botkt.manager.managers.JoinLeaveManager
     */
    val managers = CloseableList<Manager>()

    /**
     * The list of [BotCommand] the plugin will add.
     *
     * @sample org.kamiblue.botkt.command.commands.misc.ExampleCommand
     */
    val commands = CloseableList<BotCommand>()

    /**
     * The list of [BackgroundJob] the plugin will add.
     *
     * @sample org.kamiblue.botkt.command.commands.github.CounterCommand
     */
    val bgJobs = CloseableList<BackgroundJob>()

    internal fun setInfo(infoIn: PluginInfo) {
        info = infoIn
    }

    internal fun register() {
        managers.close()
        commands.close()
        bgJobs.close()

        managers.forEach(BotEventBus::subscribe)
        commands.forEach(CommandManager::register)
        bgJobs.forEach(BackgroundScope::launchLooping)
    }

    internal fun unregister() {
        managers.forEach {
            BotEventBus.unsubscribe(it)
            ListenerManager.unregister(it)
        }
        commands.forEach {
            CommandManager.unregister(it)
            ListenerManager.unregister(it)
        }
        bgJobs.forEach(BackgroundScope::cancel)
    }

    /**
     * Called when the plugin is loaded. Override / implement this method to
     * do something when the plugin is loaded.
     */
    open fun onLoad() {}

    /**
     * Called when the plugin is unloaded. Override / implement this method to
     * do something when the plugin is unloaded.
     */
    open fun onUnload() {}

    override fun equals(other: Any?) = this === other
        || (other is Plugin
        && name == other.name)

    override fun hashCode() = name.hashCode()

    override fun toString() = info.toString()

}
