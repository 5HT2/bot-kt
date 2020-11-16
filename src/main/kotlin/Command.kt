package org.kamiblue.botkt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.utils.MessageSendUtils.log
import org.kamiblue.botkt.helpers.StringHelper.firstInSentence
import org.reflections.Reflections
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author l1ving
 * @since 2020/08/18 16:30
 */
open class Command(val name: String) : LiteralArgumentBuilder<Cmd>(name) {
    val fullName = "${Main.prefix}$name"
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
    private val commandMap = HashMap<String, Command>()

    fun isCommand(name: String) = commandMap.containsKey(name.firstInSentence())

    fun getCommand(name: String) = commandMap[name.firstInSentence()]

    /**
     * Uses reflection to get a list of classes in the commands package which extend [Command]
     * and register said classes instances with Brigadier.
     */
    fun registerCommands(dispatcher: CommandDispatcher<Cmd>) {
        val commandClass = Reflections("org.kamiblue.botkt.commands").getSubTypesOf(Command::class.java)

        log("Registering commands...")

        for (clazz in commandClass) {
            val command = clazz.getField("INSTANCE").get(null) as Command
            commandMap[command.literal] = command
            println("[commands] ${command.literal} ${command.arguments}")
            dispatcher.register(command)
        }

        log("Registered commands!")
    }
}