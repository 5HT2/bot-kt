package org.kamiblue.botkt.commands

import com.google.gson.GsonBuilder
import org.kamiblue.botkt.helpers.StringHelper.writeBytes
import org.kamiblue.botkt.*
import org.kamiblue.botkt.Send.error
import org.kamiblue.botkt.Send.success
import org.kamiblue.botkt.utils.Colors

object ConfigCommand : Command("config") {
    init {
        literal("print") {
            string("name") {
                doesLaterIfHas(PermissionTypes.MANAGE_CONFIG) { context ->
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
            doesLaterIfHas(PermissionTypes.MANAGE_CONFIG) {
                message.channel.send {
                    embed {
                        title = "Config Types"
                        description = ConfigType.values()
                            .joinToString { "`${it.configPath.substring(7)}`" } // trim config/ from name
                        color = Colors.PRIMARY.color
                    }
                }
            }
        }

        literal("reload") {
            string("name") {
                doesLaterIfHas(PermissionTypes.MANAGE_CONFIG) { context ->
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
                    doesLaterIfHas(PermissionTypes.MANAGE_CONFIG) { context ->
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