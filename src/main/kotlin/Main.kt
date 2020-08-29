import CommandManager.registerCommands
import UpdateHelper.updateCheck
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kotlinx.coroutines.runBlocking
import net.ayataka.kordis.DiscordClient
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import java.awt.Color
import kotlin.system.exitProcess

fun main() = runBlocking {
    Bot().start()
}

/**
 * @author dominikaaaa
 * @since 16/08/20 17:30
 */
class Bot {
    private val dispatcher = CommandDispatcher<Cmd>()
    private var hasUpdate = false

    suspend fun start() {
        val started = System.currentTimeMillis()

        if (System.getProperty("bot-kt.create-pm2-config") == "true") {
            Pm2.createJson(Main.currentVersion)
            println("Created pm2 config!")
            exitProcess(0)
        }

        println("Starting bot!")

        updateCheck()

        val config = FileManager.readConfig<AuthConfig>(ConfigType.AUTH, false)

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
        println("Initialized bot!\nStartup took ${System.currentTimeMillis() - started}ms")
    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
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
}

object Main {
    var client: DiscordClient? = null
    const val currentVersion = "1.0.1"

    /**
     * Int colors, converted from here: https://www.shodor.org/stella2java/rgbint.html
     */
    enum class Colors(val color: Color) {
        BLUE(Color(10195199)),
        ERROR(Color(14565692)),
        WARN(Color(14595644)),
        SUCCESS(Color(3989082));
    }
}
