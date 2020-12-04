package org.kamiblue.botkt.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.utils.MessageSendUtils.log
import org.kamiblue.botkt.utils.StringUtils.firstInSentence
import org.kamiblue.commons.utils.ClassUtils
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author l1ving
 * @since 2020/08/18 16:30
 */
open class CommandOld(val name: String) : LiteralArgumentBuilder<CmdOld>(name) {
    val fullName = "${Main.prefix}$name"
    open fun getHelpUsage(): String = "`$fullName`"
}

class CmdOld(val event: MessageReceiveEvent) {

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

object CommandManagerOld {
    /* Name, Literal Command */
    private val commandMap = HashMap<String, CommandOld>()

    fun isCommand(name: String) = commandMap.containsKey(name.firstInSentence())

    fun getCommand(name: String) = commandMap[name.firstInSentence()]

    /**
     * Uses reflection to get a list of classes in the commands package which extend [CommandOld]
     * and register said classes instances with Brigadier.
     */
    fun registerCommands(dispatcher: CommandDispatcher<CmdOld>) {
        val commandClasses = ClassUtils.findClasses("org.kamiblue.botkt.command.commands", CommandOld::class.java)

        log("Registering commands...")

        for (clazz in commandClasses) {
            val command = ClassUtils.getInstance(clazz)
            commandMap[command.literal] = command
            log("${command.literal} ${command.arguments}", "[commands]")
            dispatcher.register(command)
        }

        log("Registered commands!")
    }
}