package org.kamiblue.botkt.command

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.UserConfig
import org.kamiblue.botkt.command.commands.system.ExceptionCommand
import org.kamiblue.botkt.event.BotEventBus
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.command.AbstractCommandManager
import org.kamiblue.command.utils.CommandNotFoundException
import org.kamiblue.command.utils.SubCommandNotFoundException
import org.kamiblue.commons.extension.max
import org.kamiblue.commons.utils.ClassUtils
import org.kamiblue.event.listener.asyncListener

object CommandManager : AbstractCommandManager<MessageExecuteEvent>() {

    private val commandScope = CoroutineScope(Dispatchers.Default + CoroutineName("Bot-kt Command"))

    init {
        asyncListener<MessageReceiveEvent> {
            val message = it.message.content
            if (message.isNotBlank() && message.first() == Main.prefix) {
                runCommand(it, it.message.content.substring(1))
            }
        }
    }

    fun init() {
        val commandClasses = ClassUtils.findClasses("org.kamiblue.botkt.command.commands", BotCommand::class.java)

        for (clazz in commandClasses) {
            val botCommand = ClassUtils.getInstance(clazz)
            BotEventBus.subscribe(botCommand)
            botCommand.category.commands.add(register(botCommand))
        }

        Main.logger.info("Registered ${getCommands().size} commands!")

        BotEventBus.subscribe(this)
    }

    fun runCommand(event: MessageReceiveEvent, string: String) {
        commandScope.launch {
            val args = tryParseArgument(event, string) ?: return@launch

            try {
                try {
                    invoke(MessageExecuteEvent(args, event))
                } catch (e: CommandNotFoundException) {
                    handleCommandNotFoundException(event, e)
                } catch (e: SubCommandNotFoundException) {
                    handleSubCommandNotFoundException(event, string, args, e)
                }
            } catch (e: Exception) {
                handleExceptions(event, args, e)
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
                description = "${e.message}\n" +
                    "Use the `${Main.prefix}exception` command to view the full stacktrace."
                color = Colors.ERROR.color
            }
        }
        null
    }

    private suspend fun handleCommandNotFoundException(
        event: MessageReceiveEvent,
        e: CommandNotFoundException
    ) {
        if (ConfigManager.readConfigSafe<UserConfig>(
                ConfigType.USER,
                false
            )?.unknownCommandError == true
        ) {
            event.message.channel.send {
                embed {
                    title = "Unknown Command"
                    description = "Command not found: `${e.command}`\n" +
                        "Use `${Main.prefix}help` to get a list of available commands."
                    color = Colors.ERROR.color
                }
            }
        }
    }

    private suspend fun handleSubCommandNotFoundException(
        event: MessageReceiveEvent,
        string: String,
        args: Array<String>,
        e: SubCommandNotFoundException
    ) {
        val bestCommand = e.command.finalArgs.maxByOrNull { it.countArgs(args) }

        val syntax = e.command.printArgHelp()
            .lines()
            .joinToString("\n") {
                if (it.isNotBlank() && !it.startsWith("- ")) {
                    "`${Main.prefix}$it`"
                } else {
                    it
                }
            }

        event.message.channel.send {
            embed {
                title = "Invalid Syntax: `${Main.prefix}$string`"
                if (bestCommand != null) {
                    val prediction = "`${Main.prefix}${bestCommand.printArgHelp()}`"
                    field("Did you mean?", prediction)
                }
                field("Available arguments:", syntax.max(1024))
                color = Colors.ERROR.color
            }
        }
    }

    private suspend fun handleExceptions(
        event: MessageReceiveEvent,
        args: Array<String>,
        e: Exception
    ) {
        ExceptionCommand.addException(e)
        event.message.channel.send {
            embed {
                title = "Command Exception Occurred"
                description = "The command `${args.first()}` threw an exception.\n" +
                    "Use the `${Main.prefix}exception` command to view the full stacktrace."
                field("Exception Message", e.message.toString())
                color = Colors.ERROR.color
            }
        }
    }
}
