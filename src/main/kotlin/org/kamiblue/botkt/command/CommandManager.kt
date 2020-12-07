package org.kamiblue.botkt.command

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.ConfigManager
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.UserConfig
import org.kamiblue.botkt.command.commands.ExceptionCommand
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils
import org.kamiblue.botkt.utils.StringUtils.flat
import org.kamiblue.command.AbstractCommandManager
import org.kamiblue.command.utils.CommandNotFoundException
import org.kamiblue.command.utils.SubCommandNotFoundException
import org.kamiblue.commons.utils.ClassUtils
import java.util.concurrent.ConcurrentLinkedQueue

object CommandManager : AbstractCommandManager<MessageExecuteEvent>() {

    private val executeQueue = ConcurrentLinkedQueue<suspend () -> Unit>()

    fun init() {
        val commandClasses = ClassUtils.findClasses("org.kamiblue.botkt.command.commands", BotCommand::class.java)

        MessageSendUtils.log("Registering commands...")

        for (clazz in commandClasses) {
            register(ClassUtils.getInstance(clazz))
        }

        MessageSendUtils.log("Registered ${getCommands().size} commands!")
    }

    suspend fun submit(event: MessageReceiveEvent, string: String) {
        executeQueue.add {
            runCommand(event, string)
        }
    }

    suspend fun runQueued() = coroutineScope {
        if (executeQueue.isNotEmpty()) {
            executeQueue.map {
                async {
                    it()
                }
            }.awaitAll()
            executeQueue.clear()
        }
    }

    private suspend fun runCommand(event: MessageReceiveEvent, string: String) {
        val args = tryParseArgument(event, string) ?: return

        try {
            try {
                invoke(MessageExecuteEvent(args, event))
            } catch (e: CommandNotFoundException) {
                if (ConfigManager.readConfigSafe<UserConfig>(
                        ConfigType.USER,
                        false
                    )?.unknownCommandError == true
                ) {
                    event.message.channel.send {
                        embed {
                            title = "Unknown Command"
                            description = "todo"//TODO() // reference help command
                            color = Colors.ERROR.color
                        }
                    }
                }
            } catch (e: SubCommandNotFoundException) {
                val bestCommand = e.command.finalArgs.maxByOrNull { it.countArgs(args) }
                val prediction = bestCommand?.let { best ->
                    "`${Main.prefix}${e.command.name} ${best.printArgHelp()}`"
                }

                val syntax = e.command.printArgHelp()
                    .lines()
                    .joinToString("\n") {
                        if (it.isNotBlank() && !it.startsWith("    - ")) {
                            "`${Main.prefix}${e.command.name} $it`"
                        } else {
                            it
                        }
                    }

                event.message.channel.send {
                    embed {
                        title = "Invalid Syntax: ${Main.prefix}${string}"
                        prediction?.let { prediction ->
                            field("Did you mean?", prediction)
                        }
                        field("Available arguments:", syntax.flat(1024))
                        color = Colors.ERROR.color
                    }
                }
            }
        } catch (e: Exception) {
            ExceptionCommand.addException(e)
            event.message.channel.send {
                embed {
                    title = "Command Exception Occurred"
                    description = "The command `${args.first().toLowerCase()}` threw an exception." +
                        "\nUse the `${Main.prefix}exception` command to view the full stacktrace."
                    field("Exception Message", e.message.toString())
                    color = Colors.ERROR.color
                }
            }
        }
    }

    private suspend fun tryParseArgument(event: MessageReceiveEvent, string: String) = try {
        parseArguments(string)
    } catch (e: IllegalArgumentException) {
        ExceptionCommand.addException(e)
        event.message.channel.send {
            embed {
                title = "Invalid input: ${Main.prefix}$string"
                description = "${e.message}" +
                    "\nUse the `${Main.prefix}exception` command to view the full stacktrace."
                color = Colors.ERROR.color
            }
        }
        null
    }
}

