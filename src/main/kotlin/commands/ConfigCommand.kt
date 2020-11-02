package commands

import Colors
import Command
import ConfigManager
import ConfigType
import PermissionTypes.MANAGE_CONFIG
import Send.error
import Send.success
import arg
import com.google.gson.GsonBuilder
import doesLaterIfHas
import greedyString
import helpers.StringHelper.writeBytes
import literal
import string

object ConfigCommand : Command("config") {
    init {
        literal("print") {
            string("name") {
                doesLaterIfHas(MANAGE_CONFIG) { context ->
                    val name: String = context arg "name"
                    val gson = GsonBuilder().setPrettyPrinting().create()

                    ConfigType.values().forEach { config ->
                        if (config.configPath.substring(7).toLowerCase() == name.toLowerCase()) {
                            ConfigManager.readConfig<Any>(config, false)?.let {
                                message.channel.send("```json\n" + gson.toJson(it) + "\n```")
                            } ?: message.error("Couldn't find config file, or config is in invalid format")

                            return@doesLaterIfHas
                        }
                    }
                }
            }
        }

        literal("list") {
            doesLaterIfHas(MANAGE_CONFIG) {
                message.channel.send {
                    embed {
                        title = "Config Types"
                        description = ConfigType.values()
                            .joinToString { "`${it.configPath.substring(7)}`" } // trim config/ from name
                        color = Colors.primary
                    }
                }
            }
        }

        literal("reload") {
            string("name") {
                doesLaterIfHas(MANAGE_CONFIG) { context ->
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
                            return@doesLaterIfHas
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
                    doesLaterIfHas(MANAGE_CONFIG) { context ->
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
                            return@doesLaterIfHas
                        }

                        message.success("Successfully downloaded `$name` (${size / 1000.0}KB)!")
                    }
                }
            }
        }
    }
}