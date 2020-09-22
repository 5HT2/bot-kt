package commands

import Command
import ConfigManager
import ConfigType
import Main.Colors.BLUE
import PermissionTypes
import Permissions.hasPermission
import Send.error
import Send.success
import StringHelper.writeBytes
import arg
import com.google.gson.GsonBuilder
import doesLater
import greedyString
import literal
import string

object ConfigCommand : Command("config") {
    init {
        literal("print") {
            string("name") {
                doesLater { context ->
                    if (!message.hasPermission(PermissionTypes.MANAGE_CONFIG)) {
                        return@doesLater
                    }

                    val name: String = context arg "name"
                    val gson = GsonBuilder().setPrettyPrinting().create()

                    ConfigType.values().forEach { config ->
                        if (config.configPath.substring(7).toLowerCase() == name.toLowerCase()) {
                            ConfigManager.readConfig<Any>(config, false)?.let {
                                message.channel.send("```json\n" + gson.toJson(it) + "\n```")
                            } ?: message.error("Couldn't find config file, or config is in invalid format")

                            return@doesLater
                        }
                    }
                }
            }
        }
        literal("list") {
            doesLater {
                if (!message.hasPermission(PermissionTypes.MANAGE_CONFIG)) {
                    return@doesLater
                }

                message.channel.send {
                    embed {
                        title = "Config Types"
                        description = ConfigType.values()
                            .joinToString { "`${it.configPath.substring(7)}`" } // trim config/ from name
                        color = BLUE.color
                    }
                }
            }
        }
        literal("reload") {
            string("name") {
                doesLater { context ->
                    if (!message.hasPermission(PermissionTypes.MANAGE_CONFIG)) {
                        return@doesLater
                    }

                    val name: String = context arg "name"

                    var found = false
                    ConfigType.values().forEach { config ->
                        if (config.configPath.substring(7).toLowerCase() == name.toLowerCase()) {
                            found = true
                            message.success("Reloading the `${config.configPath.substring(7)}` config")

                            /* unfortunately due to JVM limitations I cannot infer T, meaning it will not throw null if the format is invalid */
                            ConfigManager.readConfig<Any>(config, true)?.let {
                                message.success("Successfully reloaded the `${config.configPath.substring(7)}` config!")
                            } ?: message.error("Couldn't find config file, or config is in invalid format")
                            return@doesLater
                        }
                    }

                    if (!found) {
                        message.error("Couldn't find config type `$name`")
                    }
                }
            }
        }
        literal("download") {
            string("name") {
                greedyString("url") {
                    doesLater { context ->
                        if (!message.hasPermission(PermissionTypes.MANAGE_CONFIG)) {
                            return@doesLater
                        }

                        val name: String = context arg "name"
                        val url: String = context arg "url"
                        val size: Int
                        message.success("Downloading `$name`...")
                        try {
                            size = "config/$name".writeBytes(url)
                        } catch (e: Exception) {
                            message.error("Failed to download `$name`\n```${e.stackTrace.joinToString { it.toString() }}```".substring(
                                0,
                                512) + "```")
                            return@doesLater
                        }
                        message.success("Successfully downloaded `$name` (${size / 1000.0}KB)!")

                    }
                }
            }
        }
    }
}