package org.kamiblue.botkt.command.commands.system

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.plugin.Plugin
import org.kamiblue.botkt.plugin.PluginLoader
import org.kamiblue.botkt.plugin.PluginManager
import org.kamiblue.botkt.utils.*
import java.io.File
import java.net.URL

object PluginCommand : BotCommand(
    name = "plugin",
    category = Category.SYSTEM,
    description = "Manage plugins"
) {
    init {
        literal("load") {
            greedy("jar name") { nameArg ->
                executeIfHas(PermissionTypes.MANAGE_PLUGINS) {
                    val name = nameArg.value
                    val file = File("${PluginManager.pluginPath}$name")
                    if (!file.exists() || !file.extension.equals("jar", true)) {
                        channel.error("$name is not a valid jar file name!")
                    }

                    val time = System.currentTimeMillis()
                    val message = channel.normal("Loading plugin $name...")

                    val loader = PluginLoader(file)
                    val plugin = loader.load()
                    if (PluginManager.loadedPlugins.contains(plugin)) {
                        message.edit("Plugin $name already loaded!")
                        return@executeIfHas
                    }
                    PluginManager.load(loader)

                    val stopTime = System.currentTimeMillis() - time
                    message.edit {
                        description = "Loaded plugin $name, took $stopTime ms!"
                        color = Colors.SUCCESS.color
                    }
                }
            }
        }

        literal("reload") {
            greedy("plugin name") { nameArg ->
                executeIfHas(PermissionTypes.MANAGE_PLUGINS) {
                    val name = nameArg.value
                    val plugin = PluginManager.loadedPlugins[name]

                    if (plugin == null) {
                        channel.error("No plugin found for name $name")
                        return@executeIfHas
                    }

                    val time = System.currentTimeMillis()
                    val message = channel.normal("Reloading plugin $name...")

                    val file = PluginManager.pluginLoaderMap[plugin]!!.file
                    PluginManager.unload(plugin)
                    PluginManager.load(PluginLoader(file))

                    val stopTime = System.currentTimeMillis() - time
                    message.edit {
                        description = "Reloaded plugin $name, took $stopTime ms!"
                        color = Colors.SUCCESS.color
                    }
                }
            }

            executeIfHas(PermissionTypes.MANAGE_PLUGINS) {
                val time = System.currentTimeMillis()
                val message = channel.normal("Reloading all plugins...")

                PluginManager.unloadAll()
                PluginManager.loadAll(PluginManager.getLoaders())

                val stopTime = System.currentTimeMillis() - time
                message.edit {
                    description = "Reloaded plugins, took $stopTime ms!"
                    color = Colors.SUCCESS.color
                }
            }
        }

        literal("unload") {
            greedy("plugin name") { nameArg ->
                executeIfHas(PermissionTypes.MANAGE_PLUGINS) {
                    val name = nameArg.value
                    val plugin = PluginManager.loadedPlugins[name]

                    if (plugin == null) {
                        channel.error("No plugin found for name $name")
                        return@executeIfHas
                    }

                    val time = System.currentTimeMillis()
                    val message = channel.normal("Unloading plugin $name...")

                    PluginManager.unload(plugin)

                    val stopTime = System.currentTimeMillis() - time
                    message.edit {
                        description = "Unloaded plugin $name, took $stopTime ms!"
                        color = Colors.SUCCESS.color
                    }
                }
            }

            executeIfHas(PermissionTypes.MANAGE_PLUGINS) {
                val time = System.currentTimeMillis()
                val message = channel.normal("Unloading plugins...")

                PluginManager.unloadAll()

                val stopTime = System.currentTimeMillis() - time
                message.edit {
                    description = "Unloaded plugins, took $stopTime ms!"
                    color = Colors.SUCCESS.color
                }
            }
        }

        literal("download") {
            string("file name") { fileNameArg ->
                greedy("url") { urlArg ->
                    executeIfHas(PermissionTypes.MANAGE_PLUGINS, "Download a plugin") {
                        val time = System.currentTimeMillis()
                        val name = fileNameArg.value.removeSuffix(".jar") + ".jar"

                        val deferred = coroutineScope {
                            async(Dispatchers.IO) {
                                val bytes = URL(urlArg.value).readBytes()
                                File(PluginManager.pluginPath + name).writeBytes(bytes)
                            }
                        }

                        val message = channel.normal("Downloading plugin `$name` from URL <${urlArg.value}>...")

                        deferred.join()
                        val stopTime = System.currentTimeMillis() - time
                        message.edit {
                            description = "Downloaded plugin `$name`, took $stopTime ms!"
                            color = Colors.SUCCESS.color
                        }
                    }
                }
            }
        }

        literal("delete") {
            greedy("file name") { fileNameArg ->
                executeIfHas(PermissionTypes.MANAGE_PLUGINS, "Delete a plugin") {
                    val name = fileNameArg.value.removeSuffix(".jar") + ".jar"

                    val file = File(PluginManager.pluginPath + name)

                    if (!file.exists()) {
                        channel.error("Could not find a plugin file with the name `$name`")
                        return@executeIfHas
                    }

                    file.delete()
                    channel.success("Deleted plugin with file name `$name`")
                }
            }
        }

        literal("list") {
            executeIfHas(PermissionTypes.MANAGE_PLUGINS) {
                if (PluginManager.loadedPlugins.isEmpty()) {
                    channel.warn("No plugins loaded")
                } else {
                    channel.send {
                        embed {
                            title = "Loaded plugins: `${PluginManager.loadedPlugins.size}`"
                            description = PluginManager.loadedPlugins.withIndex().joinToString("\n") { (index, plugin) ->
                                "`$index`. ${plugin.name}"
                            }
                        }
                    }
                }
            }
        }

        literal("info") {
            int("index") { indexArg ->
                executeIfHas(PermissionTypes.MANAGE_PLUGINS) {
                    val index = indexArg.value
                    val plugin = PluginManager.loadedPlugins.toList().getOrNull(index)
                        ?: run {
                            channel.error("No plugin found for index: `$index`")
                            return@executeIfHas
                        }
                    val loader = PluginManager.pluginLoaderMap[plugin]!!

                    sendPluginInfo(plugin, loader)
                }
            }

            string("plugin name") { nameArg ->
                execute {
                    val name = nameArg.value
                    val plugin = PluginManager.loadedPlugins[name]
                        ?: run {
                            channel.error("No plugin found for name: `$name`")
                            return@execute
                        }
                    val loader = PluginManager.pluginLoaderMap[plugin]!!

                    sendPluginInfo(plugin, loader)
                }
            }
        }
    }

    private suspend fun MessageExecuteEvent.sendPluginInfo(plugin: Plugin, loader: PluginLoader) {
        channel.send {
            embed {
                title = "Info for plugin: $loader"
                description = plugin.toString()
                color = Colors.PRIMARY.color
            }
        }
    }
}
