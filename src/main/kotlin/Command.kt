package org.kamiblue.botkt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.Send.log
import org.kamiblue.botkt.helpers.StringHelper.firstInSentence
import org.reflections.Reflections
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author l1ving
 * @since 2020/08/18 16:30
 */
open class Command(val name: String) : LiteralArgumentBuilder<Cmd>(name) {
    val fullName = "${Main.prefix()}$name"
    open fun getHelpUsage(): String = "`$fullName`"
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

    fun isCommand(name: String) = commands.containsKey(name.firstInSentence())

    fun getCommand(name: String) = commands[name.firstInSentence()]

    fun getCommandClass(name: String) = commandClasses[name.firstInSentence()]

    /**
     * Uses reflection to get a list of classes in the commands package which extend [Command]
     * and register said classes instances with Brigadier.
     */
    @Suppress("UNCHECKED_CAST")
    fun registerCommands(dispatcher: CommandDispatcher<Cmd>) {
        val reflections = Reflections("org.kamiblue.botkt.commands")

        val subTypes: Set<Class<out Command>> = reflections.getSubTypesOf(Command::class.java)

        log("Registering commands...")

        for (command in subTypes) {
            val literalCommand = command.getField("INSTANCE").get(null) as LiteralArgumentBuilder<Cmd>
            val commandAsInstanceOfCommand = command.getField("INSTANCE").get(null) as Command
            commandClasses[literalCommand.literal] = commandAsInstanceOfCommand
            println("[commands] ${literalCommand.literal} ${commandAsInstanceOfCommand.arguments}")
            commands[literalCommand.literal] = dispatcher.register(literalCommand)
        }

        log("Registered commands!")
    }
}