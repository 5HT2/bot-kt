package org.kamiblue.botkt.command.commands.system

import com.google.gson.GsonBuilder
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.options.HasPermission
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.error
import org.kamiblue.botkt.utils.normal
import java.io.File
import java.net.URL
import kotlin.math.min

@Suppress("BlockingMethodInNonBlockingContext")
object ConfigCommand : BotCommand(
    name = "config",
    alias = arrayOf("cfg"),
    category = Category.SYSTEM,
    description = "Manage bot configurations"
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        literal("print") {
            enum<ConfigType>("type") { typeArg ->
                execute("Print a config by type", HasPermission.get(PermissionTypes.MANAGE_CONFIG)) {
                    val configType = typeArg.value

                    ConfigManager.readConfig<Any>(configType, false)?.let {
                        message.channel.send("```json\n" + gson.toJson(it) + "\n```")
                    } ?: channel.error("Couldn't find config file, or config is in invalid format")
                }
            }
        }

        literal("list") {
            execute("List the config types", HasPermission.get(PermissionTypes.MANAGE_CONFIG)) {
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
                execute("Reload a config by type", HasPermission.get(PermissionTypes.MANAGE_CONFIG)) {
                    val configType = typeArg.value

                    val message = channel.normal("Reloading the `${configType.name}` config")

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
                    execute("Download a new config by type", HasPermission.get(PermissionTypes.MANAGE_CONFIG)) {
                        val configName = typeArg.value.configPath.substring(7)
                        val message = channel.normal("Downloading `$configName`...")

                        try {
                            val bytes = URL(urlArg.value).readBytes()
                            val size = bytes.size
                            File(typeArg.value.configPath).writeBytes(bytes)

                            message.edit {
                                color = Colors.SUCCESS.color
                                description = "Successfully downloaded `$configName` (${size / 1000.0}KB)!"
                            }
                        } catch (e: Exception) {
                            val stackTrace = e.stackTrace.toList().run {
                                subList(0, min(10, this.size))
                            }
                            message.edit {
                                color = Colors.ERROR.color
                                description = "Failed to download `$configName`\n" +
                                    "```$stackTrace```"
                            }
                        }
                    }
                }
            }
        }
    }
}
