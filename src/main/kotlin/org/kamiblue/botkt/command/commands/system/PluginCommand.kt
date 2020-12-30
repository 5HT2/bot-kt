package org.kamiblue.botkt.command.commands.system

import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.plugin.PluginLoader
import org.kamiblue.botkt.plugin.PluginManager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.error
import java.io.File

object PluginCommand : BotCommand(
    name = "plugin",
    category = Category.SYSTEM,
    description = "Manage plugins"
) {
    init {
        literal("load") {
            string("jar name") { nameArg ->
                executeIfHas(PermissionTypes.REBOOT_BOT) {
                    val name = nameArg.value
                    val file = File("${PluginManager.pluginPath}$name")
                    if (!file.exists() || !file.extension.equals("jar", true)) {
                        message.channel.error("$name is not a valid jar file name!")
                    }

                    val time = System.currentTimeMillis()
                    val message = message.channel.send("Loading plugin $name...")

                    val loader = PluginLoader(file)
                    val plugin = loader.load()
                    if (PluginManager.loadedPlugins.contains(plugin)) {
                        message.edit("Plugin $name already loaded!")
                        return@executeIfHas
                    }
                    PluginManager.load(loader)

                    val stopTime = System.currentTimeMillis() - time
                    message.edit("Loaded plugin $name, took $stopTime ms!")
                }
            }
        }

        literal("reload") {
            string("plugin name") { nameArg ->
                executeIfHas(PermissionTypes.REBOOT_BOT) {
                    val name = nameArg.value
                    val plugin = PluginManager.loadedPlugins[name]

                    if (plugin == null) {
                        message.channel.error("No plugin found for name $name")
                        return@executeIfHas
                    }

                    val time = System.currentTimeMillis()
                    val message = message.channel.send("Reloading plugin $name...")

                    val file = PluginManager.pluginLoaderMap[plugin]!!.file
                    PluginManager.unload(plugin)
                    PluginManager.load(PluginLoader(file))

                    val stopTime = System.currentTimeMillis() - time
                    message.edit("Reloaded plugin $name, took $stopTime ms!")
                }
            }

            executeIfHas(PermissionTypes.REBOOT_BOT) {
                val time = System.currentTimeMillis()
                val message = message.channel.send("Reloading plugins...")

                PluginManager.unloadAll()
                PluginManager.loadAll(PluginManager.getLoaders())

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
                    val message = message.channel.send("Unloading plugin $name...")

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
                        "`$index`. $it"
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
