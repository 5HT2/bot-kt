package org.kamiblue.botkt.command.commands

import com.google.gson.GsonBuilder
import org.kamiblue.botkt.*
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.success
import org.kamiblue.botkt.utils.StringUtils.writeBytes
import kotlin.math.min

object ConfigCommand : Command("config") {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        literal("print") {
            string("name") {
                doesLaterIfHas(PermissionTypes.MANAGE_CONFIG) { context ->
                    val name: String = context arg "name"

                    ConfigType.values().find {
                        it.configPath.substring(7).toLowerCase() == name.toLowerCase()
                    }?.let { config ->
                        ConfigManager.readConfig<Any>(config, false)?.let {
                            message.channel.send("```json\n" + gson.toJson(it) + "\n```")
                        } ?: message.error("Couldn't find config file, or config is in invalid format")
                    }
                }
            }
        }

        literal("list") {
            doesLaterIfHas(PermissionTypes.MANAGE_CONFIG) {
                message.channel.send {
                    embed {
                        title = "Config Types"
                        description = ConfigType.values().joinToString { "`${it.configPath.substring(7)}`" } // trim config/ from name
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

                        message.success("Downloading `$name`...")

                        try {
                            val size = "config/$name".writeBytes(url)
                            message.success("Successfully downloaded `$name` (${size / 1000.0}KB)!")
                        } catch (e: Exception) {
                            val stackTrace = e.stackTrace.toList().run {
                                subList(0, min(10, this.size))
                            }
                            message.error(
                                "Failed to download `$name`\n" +
                                    "```${stackTrace}```"
                            )
                        }
                    }
                }
            }
        }
    }
}