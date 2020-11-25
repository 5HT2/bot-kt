package org.kamiblue.botkt

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.utils.MessageSendUtils.log
import org.kamiblue.botkt.utils.StringUtils.firstInSentence
import org.kamiblue.commons.utils.ReflectionUtils
import org.kamiblue.commons.utils.ReflectionUtils.instance
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

    private val asyncQueue = ConcurrentLinkedQueue<suspend MessageReceiveEvent.() -> Unit>()

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
        val commandClasses = ReflectionUtils.getSubclassOfFast<Command>("org.kamiblue.botkt.commands")

        log("Registering commands...")

        for (clazz in commandClasses) {
            val command = clazz.instance
            commandMap[command.literal] = command
            println("[commands] ${command.literal} ${command.arguments}")
            dispatcher.register(command)
        }

        log("Registered commands!")
    }
}