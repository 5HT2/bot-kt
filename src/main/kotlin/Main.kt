import CommandManager.registerCommands
import ConfigManager.readConfigSafe
import Main.ready
import Send.log
import UpdateHelper.updateCheck
import UpdateHelper.writeCurrentVersion
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kotlinx.coroutines.*
import net.ayataka.kordis.DiscordClient
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.server.enums.ActivityType
import net.ayataka.kordis.entity.server.enums.UserStatus
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.l1ving.api.download.Download
import utils.request
import java.awt.Color
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import java.awt.Color
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess
import ConfigManager.readConfigSafe
import kotlinx.coroutines.*
import utils.*
import commands.DownloadCountCommand

fun main() = runBlocking {
    Main.process = launch {
        Bot().start()
        while(true){
            withTimeout(getUpdateInterval()){ DownloadCountCommand.updateChannel() }
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

        writeCurrentVersion()
        updateCheck()

        val config = ConfigManager.readConfig<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            log("Bot token not found, make sure your file is formatted correctly!. \nExiting...")
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
                val channel = Main.client!!.servers.find(userConfig.primaryServerId)!!.textChannels.findByName(it)
                channel?.send {
                    embed {
                        title = "Startup"
                        description = initialization
                        color = Colors.success
                    }
                }
            }
        }

        ready = true
        log(initialization)
    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
        if (!ready || event.message.content.isEmpty()) return // message can be empty on images, embeds and other attachments

        val message = if (event.message.content[0] == ';') event.message.content.substring(1) else return
        val cmd = Cmd(event)

        try {
            val exit = dispatcher.execute(message, cmd)
            cmd.file(event)
            if (exit != 0) log("(executed with exit code $exit)")
        } catch (e: CommandSyntaxException) {
            if (CommandManager.isCommand(message)) {
                val command = CommandManager.getCommandClass(message)!!
                cmd.event.message.channel.send {
                    embed {
                        title = "Invalid Syntax: $message"
                        description = "**${e.message}**\n\n${command.getHelpUsage()}"
                        color = Colors.error
                    }
                }
            }
        }
    }
}

object Main {
    var process: Job? = null
    var client: DiscordClient? = null
    var ready = false
    const val currentVersion = "1.1.1"

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
}
