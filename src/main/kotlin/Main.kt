import Main.currentVersion
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import kotlinx.coroutines.runBlocking
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.reflections.Reflections
import java.awt.Color
import java.io.File


fun main() = runBlocking {
    Bot().start()
}

class Bot {
    private val dispatcher = CommandDispatcher<Cmd>()
    private var hasUpdate = false

    suspend fun start() {
        val started = System.currentTimeMillis()
        println("Starting bot!")

        updateCheck()

        val config = FileManager.readConfig<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            println("Bot token not found, make sure your file is formatted correctly!. \nExiting...")
            return
        }

        val client = Kordis.create {
            token = config.botToken

            // Annotation based Event Listener
            addListener(this@Bot)
        }

        registerCommands()
        println("Initialized bot!\n" +
                "Startup took ${System.currentTimeMillis() - started}ms")
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
                cmd.event.message.channel.send {
                    embed {
                        title = "Invalid Syntax!"
                        description = e.message
                        color = Main.Colors.ERROR.color
                    }
                }
            }
        }
    }

    private fun updateCheck() {
        if (File("noUpdateCheck").exists()) return
        val versionConfig = FileManager.readConfig<VersionConfig>(ConfigType.VERSION, false)

        if (versionConfig?.version == null) {
            println("Couldn't access remote version when checking for update")
            return
        }

        if (versionConfig.version != currentVersion) {
            println("Not up to date:\nCurrent version: $currentVersion\nLatest Version: ${versionConfig.version}\n")
        } else {
            println("Up to date! Running on $currentVersion")
        }
    }

    /**
     * Uses reflection to get a list of classes in the commands package which extend [Command]
     * and register said classes's instances with Brigadier.
     */
    private fun registerCommands() {
        val reflections = Reflections("commands")

        val subTypes: Set<Class<out Command>> = reflections.getSubTypesOf(Command::class.java)

        println("Registering commands...")

        for (command in subTypes) {
            val literalCommand = command.getField("INSTANCE").get(null) as LiteralArgumentBuilder<Cmd>
            CommandManager.commands[literalCommand.literal] = dispatcher.register(literalCommand)
        }

        var registeredCommands = ""
        CommandManager.commands.forEach { entry -> registeredCommands += "\n> ;${entry.key}" }

        println("Registered commands!$registeredCommands\n")
    }
}

object Main {
    const val currentVersion = "1.0.0"
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
