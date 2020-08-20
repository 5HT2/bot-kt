import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import commands.ArchiveCommand
import commands.ExampleCommand
import kotlinx.coroutines.coroutineScope
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
/*            addHandler<UserJoinEvent> {
                println(it.member.name + " has joined")
            }*/

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
        dispatcher.register(ArchiveCommand)
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
//            cmd.event.message.channel.send("Syntax error:\n```\n${e.message}\n```")
        }
    }
}
