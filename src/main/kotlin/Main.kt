import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import commands.ExampleCommand
import kotlinx.coroutines.runBlocking
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import net.ayataka.kordis.event.events.server.user.UserJoinEvent

fun main() = runBlocking {
    Bot().start()
}

class Bot {
    private val dispatcher = CommandDispatcher<Cmd>()
    suspend fun start() {
        println("Starting bot!")
        val config = FileManager.readConfig<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            println("Bot token not found, make sure your file is formatted correctly!. \nExiting...")
            return
        }

        val client = Kordis.create {
            token = config.botToken

            // Simple Event Handler
            addHandler<UserJoinEvent> {
                println(it.member.name + " has joined")
            }

            // Annotation based Event Listener
            addListener(this@Bot)
        }

        registerCommands()
        println("Initialized bot!")
    }

    /**
     * TODO: use annotations for Commands and automatically register
     */
    fun registerCommands() {
        dispatcher.register(ExampleCommand)
    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
        try {
            val exit = dispatcher.execute(event.message.content, Cmd(event))
            println("(executed with exit code $exit)")
        } catch (e: CommandSyntaxException) {
            println("You have a syntax error: ${e.message}")
        }
    }
}
