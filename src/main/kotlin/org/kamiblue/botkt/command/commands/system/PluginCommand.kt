package org.kamiblue.botkt.command.commands.system

import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.plugin.PluginManager

object PluginCommand : BotCommand(
    name = "plugin",
    category = Category.SYSTEM,
    description = "Manage plugins"
) {
    init {
        literal("reload") {
            executeIfHas(PermissionTypes.REBOOT_BOT) {
                val time = System.currentTimeMillis()
                val message = message.channel.send("Reloading plugins...")
                PluginManager.unloadAll()
                PluginManager.loadAll(PluginManager.preLoad())
                val stopTime = System.currentTimeMillis() - time
                message.edit("Reloaded plugins, took $stopTime ms!")
            }
        }

        literal("unload") {
            executeIfHas(PermissionTypes.REBOOT_BOT) {
                val time = System.currentTimeMillis()
                val message = message.channel.send("Unloading plugins...")
                PluginManager.unloadAll()
                val stopTime = System.currentTimeMillis() - time
                message.edit("Unloaded plugins, took $stopTime ms!")
            }
        }
    }
}