package org.kamiblue.botkt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kotlinx.coroutines.delay
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.server.enums.ActivityType
import net.ayataka.kordis.entity.server.enums.UserStatus
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.command.CmdOld
import org.kamiblue.botkt.command.CommandManagerOld
import org.kamiblue.botkt.helpers.UpdateHelper
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.StringUtils.firstInSentence
import org.kamiblue.botkt.utils.StringUtils.flat

/**
 * @author l1ving
 * @since 16/08/20 17:30
 */
object Bot {
    private val dispatcher = CommandDispatcher<CmdOld>()

    suspend fun start() {
        val started = System.currentTimeMillis()

        MessageSendUtils.log("Starting bot!")

        UpdateHelper.writeVersion(Main.currentVersion)
        UpdateHelper.updateCheck()

        val config = ConfigManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            MessageSendUtils.log("Bot token not found, make sure your file is formatted correctly!. \nExiting...")
            Main.exit()
            return
        }

        Main.client = Kordis.create {
            token = config.botToken
            // Annotation based Event Listener
            addListener(this@Bot)
        }

        CommandManagerOld.registerCommands(dispatcher)

        val initialization = "Initialized bot!\nRunning on ${Main.currentVersion}\nStartup took ${System.currentTimeMillis() - started}ms"
        val userConfig = ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)

        userConfig?.statusMessage?.let {
            var type = ActivityType.UNKNOWN
            userConfig.statusMessageType.let {
                ActivityType.values().forEach { lType -> if (lType.id == it) type = lType }
            }

            Main.client.updateStatus(UserStatus.ONLINE, type, it)
        }

        delay(2000) // Discord API is really stupid and doesn't give you the information you need right away, hence delay needed

        userConfig?.startUpChannel?.let {
            if (userConfig.primaryServerId == null) {
                Main.client.servers.forEach { chit ->
                    delay(100) // we don't want to hit the message rate limit, 10 messages a second should be fine
                    chit.textChannels.findByName(it)?.send {
                        embed {
                            title = "Startup"
                            description = initialization
                            color = Colors.SUCCESS.color
                        }
                    }
                }
            } else {
                val channel = Main.client.servers.find(userConfig.primaryServerId)?.textChannels?.findByName(it)
                channel?.send {
                    embed {
                        title = "Startup"
                        description = initialization
                        color = Colors.SUCCESS.color
                    }
                }
            }
        }

        Main.ready = true
        MessageSendUtils.log(initialization)
    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
        if (!Main.ready || event.message.content.isEmpty()) return // message can be empty on images, embeds and other attachments

        val message = if (event.message.content[0] == Main.prefix) event.message.content.substring(1) else return
        val cmd = CmdOld(event)

        try {
            try {
                val exit = dispatcher.execute(message, cmd)
                cmd.file(event)
                if (exit != 0) MessageSendUtils.log("(executed with exit code $exit)")
            } catch (e: CommandSyntaxException) {
                if (CommandManagerOld.isCommand(message)) {
                    val usage = CommandManagerOld.getCommand(message)?.getHelpUsage()
                    cmd.event.message.channel.send {
                        embed {
                            title = "Invalid Syntax: ${Main.prefix}$message"
                            description = "${e.message}${
                                usage?.let {
                                    "\n\n$it"
                                } ?: ""
                            }"
                            color = Colors.ERROR.color
                        }
                    }
                } else if (ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)?.unknownCommandError == true) {
                    cmd.event.message.channel.send {
                        embed {
                            title = "Unknown Command: ${Main.prefix}${message.firstInSentence()}"
                            color = Colors.ERROR.color
                        }
                    }

                }
            }
        } catch (e: Exception) {
            event.message.error("```\n${e.stackTraceToString()}\n".flat(2045) + "```") // TODO: proper command to view stacktraces
        }
    }
}