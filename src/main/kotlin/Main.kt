import CommandManager.registerCommands
import Main.ready
import UpdateHelper.updateCheck
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.ayataka.kordis.DiscordClient
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.server.enums.ActivityType
import net.ayataka.kordis.entity.server.enums.UserStatus
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import java.awt.Color
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.*
import ConfigManager.readConfigSafe
import Send.error
import net.ayataka.kordis.entity.message.*
import net.ayataka.kordis.entity.server.channel.voice.*

fun main() = runBlocking {
    Main.process = launch {
        Bot().start()
    }
}

/**
 * @author dominikaaaa
 * @since 16/08/20 17:30
 */
class Bot {
    private val dispatcher = CommandDispatcher<Cmd>()

    suspend fun start() {
        val started = System.currentTimeMillis()

        println("Starting bot!")

        writeCurrentVersion()
        updateCheck()

        val config = ConfigManager.readConfig<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            println("Bot token not found, make sure your file is formatted correctly!. \nExiting...")
            return
        }

        Main.client = Kordis.create {
            token = config.botToken
            // Annotation based Event Listener
            addListener(this@Bot)
        }

        registerCommands(dispatcher)

        val initialization = "Initialized bot!\nRunning on ${Main.currentVersion}\nStartup took ${System.currentTimeMillis() - started}ms"
        val userConfig = ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)

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
                            color = Main.Colors.SUCCESS.color
                        }
                    }
                }
            } else {
                val channel = Main.client!!.servers.find(userConfig.primaryServerId)!!.textChannels.findByName(it)
                channel?.send {
                    embed {
                        title = "Startup"
                        description = initialization
                        color = Main.Colors.SUCCESS.color
                    }
                }
            }
        }

        ready = true
        println(initialization)
    }

    private fun writeCurrentVersion() {
        val path = Paths.get(System.getProperty("user.dir"))
        val file = Paths.get("$path/currentVersion")

        if (!File(file.toString()).exists()) {
            Files.newBufferedWriter(file).use {
                it.write(Main.currentVersion)
            }
        }
    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
        if (!ready || event.message.content.isEmpty()) return // message can be empty on images, embeds and other attachments

        val message = if (event.message.content[0] == ';') event.message.content.substring(1) else return
        val cmd = Cmd(event)

        try {
            val exit = dispatcher.execute(message, cmd)
            cmd.file(event)
            if (exit != 0) println("(executed with exit code $exit)")
        } catch (e: CommandSyntaxException) {
            if (CommandManager.isCommand(message)) {
                val command = CommandManager.getCommandClass(message)!!
                cmd.event.message.channel.send {
                    embed {
                        title = "Invalid Syntax: $message"
                        description = "**${e.message}**\n\n${command.getHelpUsage()}"
                        color = Main.Colors.ERROR.color
                    }
                }
            }
        }
    }
    /**
     * @author sourTaste000(IcyChungus)
     * @module downloadCounter
     * @since 9/22/2020
     */
    suspend fun updateChannel() {
        val interval = TimeUnit.MINUTES.toMillis(readConfigSafe<UserConfig>(ConfigType.USER, false)?.updateInterval ?: 10)
        val timer = Timer()
        val task = object: TimerTask(){
            override fun run() {
                TODO("I have no idea how to fetch a voice channel from cache")
            }
        }
        timer.schedule(task, interval) //default 10 minutes

        suspend fun getReleaseChannel(message: Message): Long? {
            val releaseChannel = readConfigSafe<UserConfig>(ConfigType.USER, false)?.downloadChannel
            if (releaseChannel == null){
                message.error("Release channel not found in config! Using default channel...")
                return 743150116516528159
            }
            return releaseChannel
        }

        suspend fun getUpdateInterval(message: Message): Long? {
            val updateInterval = readConfigSafe<UserConfig>(ConfigType.USER, false)?.updateInterval
            if (updateInterval == null){
                message.error("Update interval not found in config! Using default interval...")
                return 10
            }
            return updateInterval
        }

        suspend fun getSecondaryDownloadChannel(message: Message): Long? {
            val secondaryUpdateInterval = readConfigSafe<UserConfig>(ConfigType.USER, false)?.secondaryDownloadChannel
            if (secondaryUpdateInterval == null){
                message.error("Secondary download channel not found in config! Using default channel...")
                return 743150116516528159
            }
            return secondaryUpdateInterval
        }
    }
}

object Main {
    var process: Job? = null
    var client: DiscordClient? = null
    var ready = false
    const val currentVersion = "1.1.0"

    enum class Colors(val color: Color) {
        BLUE(Color(155, 144, 255)),
        ERROR(Color(222, 65, 60)),
        WARN(Color(222, 182, 60)),
        SUCCESS(Color(60, 222, 90));
    }
}
