package org.kamiblue.botkt.plugin

import org.kamiblue.botkt.BackgroundJob
import org.kamiblue.botkt.BackgroundScope
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.event.BotEventBus
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.CloseableList
import org.kamiblue.commons.interfaces.Nameable
import org.kamiblue.event.ListenerManager

abstract class Plugin(
    override val name: String,
    val author: String,
    val version: String
) : Nameable {
    val managers = CloseableList<Manager>()
    val commands = CloseableList<BotCommand>()
    val backgroundJobs = CloseableList<BackgroundJob>()

    internal fun register() {
        managers.close()
        commands.close()
        backgroundJobs.close()

        managers.forEach {
            BotEventBus.subscribe(it)
        }
        commands.forEach {
            CommandManager.register(it)
        }
        backgroundJobs.forEach {
            BackgroundScope.launchLooping(it)
        }
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
        backgroundJobs.forEach {
            BackgroundScope.cancel(it)
        }
    }

    abstract fun onLoad()
    abstract fun onUnload()

    override fun equals(other: Any?) = this === other
        || (other is Plugin
        && name == other.name)

    override fun hashCode() = name.hashCode()

}