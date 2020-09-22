import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.reflections.Reflections
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author dominikaaaa
 * @since 2020/08/18 16:30
 */
open class Command(val name: String) : LiteralArgumentBuilder<Cmd>(name) {
    open fun getHelpUsage(): String = "`;$name`"
}

class Cmd(val event: MessageReceiveEvent) {

    private var asyncQueue: ConcurrentLinkedQueue<suspend MessageReceiveEvent.() -> Unit> = ConcurrentLinkedQueue()

    infix fun later(block: suspend MessageReceiveEvent.() -> Unit) {
        asyncQueue.add(block)
    }

    suspend fun file(event: MessageReceiveEvent) = coroutineScope {
        asyncQueue.map {
            async {
                event.it()
            }
        }.awaitAll()
    }
}

object CommandManager {
    /* Name, Literal Command */
    private val commands = hashMapOf<String, LiteralCommandNode<Cmd>>()
    private val commandClasses = hashMapOf<String, Command>()

    fun isCommand(name: String) = commands.containsKey(name)

    fun getCommand(name: String) = commands[name]

    fun getCommandClass(name: String) = commandClasses[name]

    /**
     * Uses reflection to get a list of classes in the commands package which extend [Command]
     * and register said classes instances with Brigadier.
     */
    fun registerCommands(dispatcher: CommandDispatcher<Cmd>) {
        val reflections = Reflections("commands")

        val subTypes: Set<Class<out Command>> = reflections.getSubTypesOf(Command::class.java)

        println("Registering commands...")

        for (command in subTypes) {
            val literalCommand = command.getField("INSTANCE").get(null) as LiteralArgumentBuilder<Cmd>
            val commandAsInstanceOfCommand = command.getField("INSTANCE").get(null) as Command
            commandClasses[literalCommand.literal] = commandAsInstanceOfCommand
            commands[literalCommand.literal] = dispatcher.register(literalCommand)
        }

        var registeredCommands = ""
        commands.forEach { entry -> registeredCommands += "\n> ;${entry.key}" }

        println("Registered commands!$registeredCommands\n")
    }
}