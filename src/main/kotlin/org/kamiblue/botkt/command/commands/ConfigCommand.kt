package org.kamiblue.botkt.command.commands

import com.google.gson.GsonBuilder
import org.kamiblue.botkt.ConfigManager
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.normal
import org.kamiblue.botkt.utils.StringUtils.writeBytes
import kotlin.math.min

object ConfigCommand : BotCommand(
    name = "config",
    alias = arrayOf("cfg"),
    description = "Manage configurations of the bot"
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        literal("print") {
            enum<ConfigType>("type") { typeArg ->
                executeIfHas(PermissionTypes.MANAGE_CONFIG) {
                    val configType = typeArg.value

                    ConfigManager.readConfig<Any>(configType, false)?.let {
                        message.channel.send("```json\n" + gson.toJson(it) + "\n```")
                    } ?: message.error("Couldn't find config file, or config is in invalid format")
                }
            }
        }

        literal("list") {
            executeIfHas(PermissionTypes.MANAGE_CONFIG) {
                message.channel.send {
                    embed {
                        title = "Config Types"
                        description = ConfigType.values().joinToString { "`${it.name}`" }
                        color = Colors.PRIMARY.color
                    }
                }
            }
        }

        literal("reload") {
            enum<ConfigType>("type") { typeArg ->
                executeIfHas(PermissionTypes.MANAGE_CONFIG) {
                    val configType = typeArg.value

                    val message = message.normal("Reloading the `${configType.name}` config")

                    /* unfortunately due to JVM limitations I cannot infer T, meaning it will not throw null if the format is invalid */
                    ConfigManager.readConfig<Any>(configType, true)?.let {
                        message.edit {
                            color = Colors.SUCCESS.color
                            description = "Successfully reloaded the `${configType.name}` config!"
                        }
                    } ?: message.edit {
                        color = Colors.ERROR.color
                        description = "Couldn't find config file, or config is in invalid format"
                    }
                }
            }
        }

        literal("download") {
            enum<ConfigType>("name") { typeArg ->
                greedy("url") { urlArg ->
                    executeIfHas(PermissionTypes.MANAGE_CONFIG) {
                        val configType = typeArg.value
                        val url = urlArg.value

                        val message = message.normal("Downloading `${configType.name}`...")

                        try {
                            val size = "config/$name".writeBytes(url)
                            message.edit {
                                color = Colors.SUCCESS.color
                                description = "Successfully downloaded `$name` (${size / 1000.0}KB)!"
                            }
                        } catch (e: Exception) {
                            val stackTrace = e.stackTrace.toList().run {
                                subList(0, min(10, this.size))
                            }
                            message.edit {
                                color = Colors.ERROR.color
                                description = "Failed to download `$name`\n" +
                                    "```${stackTrace}```"
                            }
                        }
                    }
                }
            }
        }
    }
}
