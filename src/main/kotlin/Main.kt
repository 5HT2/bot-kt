import CommandManager.registerCommands
import Main.ready
import Send.log
import UpdateHelper.updateCheck
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
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
import kotlin.system.exitProcess
import java.util.concurrent.*
import ConfigManager.readConfigSafe
import kotlinx.coroutines.*
import utils.request
import org.l1ving.api.download.*
import java.io.FileNotFoundException

fun main() = runBlocking {
    Main.process = launch {
        Bot().start()
        while(true){
            withTimeout(Bot().getUpdateInterval()){
                Bot().updateChannel()
            }
        }
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
    /**
     * @author sourTaste000(IcyChungus)
     * @module downloadCounter
     * @since 9/22/2020
     */
    suspend fun updateChannel() {
        // TODO: Shitty code please fix
        val interval = TimeUnit.MINUTES.toMillis(getUpdateInterval())
        val server = Main.client?.servers?.find(getServerId())
        val releaseChannel = server?.voiceChannels?.find(getReleaseChannel())
        val secondaryReleaseChannel = server?.voiceChannels?.find(getSecondaryReleaseChannel())
        val releaseCount = request<Download>(getToken(), "https://api.github.com/repos/kami-blue/client/releases?per_page=200")
        val nightlyCount = request<Download>(getToken(), "https://api.github.com/repos/kami-blue/nightly-releases/releases?per_page=200")
        var totalCount: Long = 0
        secondaryReleaseChannel?.edit { name = "${nightlyCount[0].assets[0].download_count} Nightly Downloads" }
        for(i in nightlyCount){
            for(j in i.assets){
                totalCount += j.download_count
            }
        }
        for(i in releaseCount){
            for(j in i.assets){
                totalCount += j.download_count
            }
        }
        releaseChannel?.edit { name = "$totalCount Total Downloads" }
        delay(interval)
    }

    fun getReleaseChannel(): Long {
        val releaseChannel = readConfigSafe<UserConfig>(ConfigType.USER, false)?.downloadChannel
        if (releaseChannel == null){
            println("ERROR! Release channel not found in config! Using default channel...")
            return 743240299069046835
        }
        return releaseChannel
    }

    fun getUpdateInterval(): Long {
        val updateInterval = readConfigSafe<UserConfig>(ConfigType.USER, false)?.updateInterval
        if (updateInterval == null){
            println("ERROR! Update interval not found in config! Using default interval...")
            return 10
        }
        return updateInterval
    }

    fun getSecondaryReleaseChannel(): Long {
        val secondaryUpdateInterval = readConfigSafe<UserConfig>(ConfigType.USER, false)?.secondaryDownloadChannel
        if (secondaryUpdateInterval == null){
            println("ERROR! Secondary download channel not found in config! Using default channel...")
            return 744072202869014571
        }
        return secondaryUpdateInterval
    }

    fun getServerId(): Long {
        val serverId = readConfigSafe<UserConfig>(ConfigType.USER, false)?.primaryServerId
        if (serverId == null){
            println("ERROR! Primary server ID not found in config! Using default ID...")
            return 573954110454366214
        }
        return serverId
    }
    fun getToken(): String {
        val token = readConfigSafe<AuthConfig>(ConfigType.AUTH, false)?.githubToken
        if (token == null) {
            println("ERROR! Github token not found in config! Stopping...")
            throw FileNotFoundException("Please provide a github token in the token file.")
        }
        return token
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
