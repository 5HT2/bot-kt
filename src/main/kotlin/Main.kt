package org.kamiblue.botkt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kotlinx.coroutines.*
import net.ayataka.kordis.DiscordClient
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.server.enums.ActivityType
import net.ayataka.kordis.entity.server.enums.UserStatus
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.CommandManager.registerCommands
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.Send.error
import org.kamiblue.botkt.Send.log
import org.kamiblue.botkt.commands.CapeCommand
import org.kamiblue.botkt.commands.CounterCommand
import org.kamiblue.botkt.helpers.StringHelper.firstInSentence
import org.kamiblue.botkt.helpers.StringHelper.flat
import org.kamiblue.botkt.helpers.UpdateHelper
import java.awt.Color
import kotlin.system.exitProcess

fun main() = runBlocking {
    Main.process = launch {
        Bot().start()
    }

    Main.counterProcess = launch {
        while (isActive) {
            delay(configUpdateInterval())
            CounterCommand.updateChannel()
        }
    }

    Main.capeSaveProcess = launch {
        while (isActive) {
            delay(60000) // 1 minute
            CapeCommand.save()
        }
    }

    Main.capeCommitProcess = launch {
        while (isActive) {
            delay(60010) // 1 minute
            CapeCommand.commit()
        }
    }
}

/**
 * @author l1ving
 * @since 16/08/20 17:30
 */
class Bot {
    private val dispatcher = CommandDispatcher<Cmd>()

    suspend fun start() {
        val started = System.currentTimeMillis()

        log("Starting bot!")

        UpdateHelper.writeCurrentVersion()
        UpdateHelper.updateCheck()

        val config = readConfigSafe<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            log("Bot token not found, make sure your file is formatted correctly!. \nExiting...")
            Main.exit()
            return
        }

        Main.client = Kordis.create {
            token = config.botToken
            // Annotation based Event Listener
            addListener(this@Bot)
        }

        registerCommands(dispatcher)

        val initialization = "Initialized bot!\nRunning on ${Main.currentVersion}\nStartup took ${System.currentTimeMillis() - started}ms"
        val userConfig = readConfigSafe<UserConfig>(ConfigType.USER, false)

        userConfig?.statusMessage?.let {
            var type = ActivityType.UNKNOWN
            userConfig.statusMessageType.let {
                ActivityType.values().forEach { lType -> if (lType.id == it) type = lType }
            }

            Main.client!!.updateStatus(UserStatus.ONLINE, type, it)
        }

        delay(2000) // Discord API is really stupid and doesn't give you the information you need right away, hence delay needed

        userConfig?.startUpChannel?.let {
            if (userConfig.primaryServerId == null) {
                Main.client!!.servers.forEach { chit ->
                    delay(100) // we don't want to hit the message rate limit, 10 messages a second should be fine
                    chit.textChannels.findByName(it)?.send {
                        embed {
                            title = "Startup"
                            description = initialization
                            color = Colors.success
                        }
                    }
                }
            } else {
                val channel = Main.client?.servers?.find(userConfig.primaryServerId)?.textChannels?.findByName(it)
                channel?.send {
                    embed {
                        title = "Startup"
                        description = initialization
                        color = Colors.success
                    }
                }
            }
        }

        Main.ready = true
        log(initialization)
    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
        if (!Main.ready || event.message.content.isEmpty()) return // message can be empty on images, embeds and other attachments

        val message = if (event.message.content[0] == Main.prefix()) event.message.content.substring(1) else return
        val cmd = Cmd(event)

        try {
            try {
                val exit = dispatcher.execute(message, cmd)
                cmd.file(event)
                if (exit != 0) log("(executed with exit code $exit)")
            } catch (e: CommandSyntaxException) {
                if (CommandManager.isCommand(message)) {
                    val usage = CommandManager.getCommandClass(message)?.getHelpUsage()
                    cmd.event.message.channel.send {
                        embed {
                            title = "Invalid Syntax: ${Main.prefix()}$message"
                            description = "${e.message}${
                                usage?.let {
                                    "\n\n$it"
                                } ?: ""
                            }"
                            color = Colors.error
                        }
                    }
                } else if (readConfigSafe<UserConfig>(ConfigType.USER, false)?.unknownCommandError == true) {
                    cmd.event.message.channel.send {
                        embed {
                            title = "Unknown Command: ${Main.prefix()}${message.firstInSentence()}"
                            color = Colors.error
                        }
                    }

                }
            }
        } catch (e: Exception) {
            event.message.error("```\n${e.getStackTraceAsString()}\n".flat(2045) + "```") // TODO: proper command to view stacktraces
        }
    }
}

object Main {
    var process: Job? = null
    var counterProcess: Job? = null
    var capeSaveProcess: Job? = null
    var capeCommitProcess: Job? = null
    var client: DiscordClient? = null
    var ready = false
    const val currentVersion = "v1.2.9"

    private var defaultPrefix: Char? = null

    fun prefix(): Char {
        if (defaultPrefix == null) {
            defaultPrefix = readConfigSafe<UserConfig>(ConfigType.USER, false)?.prefix ?: ';'
        }
        return defaultPrefix!! // cannot be null, as it was just assigned
    }

    fun exit() {
        process!!.cancel()
        exitProcess(0)
    }
}

object Colors {
    val primary = Color(155, 144, 255)
    val error = Color(222, 65, 60)
    val warn = Color(222, 182, 60)
    val success = Color(60, 222, 90)
    val mergedPullRequest = Color(100, 29, 188)
}
