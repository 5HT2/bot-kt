package org.kamiblue.botkt.command.commands.system

import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.plugin.PluginManager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageUtils.error

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
            string("plugin name") { nameArg ->
                executeIfHas(PermissionTypes.REBOOT_BOT) {
                    val name = nameArg.value
                    val plugin = PluginManager.loadedPlugins[name]

                    if (plugin == null) {
                        message.channel.error("No plugin found for name $name")
                        return@executeIfHas
                    }

                    val time = System.currentTimeMillis()
                    val message = message.channel.send("Unloading plugins $name...")
                    PluginManager.unload(plugin)
                    val stopTime = System.currentTimeMillis() - time
                    message.edit("Unloaded plugin $name, took $stopTime ms!")
                }
            }

            executeIfHas(PermissionTypes.REBOOT_BOT) {
                val time = System.currentTimeMillis()
                val message = message.channel.send("Unloading plugins...")
                PluginManager.unloadAll()
                val stopTime = System.currentTimeMillis() - time
                message.edit("Unloaded plugins, took $stopTime ms!")
            }
        }

        literal("list") {
            executeIfHas(PermissionTypes.REBOOT_BOT) {
                val string = if (PluginManager.loadedPlugins.isEmpty()) {
                    "No plugin loaded"
                } else {
                    PluginManager.loadedPlugins.withIndex().joinToString("\n") { (index, it) ->
                        "`$index`. Name: ${it.name}, Version: ${it.version}, Author: ${it.author}"
                    }
                }
                message.channel.send {
                    embed {
                        title = "Loaded plugins"
                        description = string
                        color = Colors.SUCCESS.color
                    }
                }
            }
        }
    }
}