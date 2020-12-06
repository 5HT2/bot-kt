package org.kamiblue.botkt.command

import com.mojang.brigadier.exceptions.CommandSyntaxException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.*
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.StringUtils.firstInSentence
import org.kamiblue.botkt.utils.StringUtils.flat
import org.kamiblue.command.AbstractCommandManager
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
        try {
            val args = parseArguments(string)

            try {
                invoke(MessageExecuteEvent(args, event))
            } catch (e: IllegalArgumentException) {
                event.message.channel.send {
                    embed {
                        title = "Invalid input: ${Main.prefix}$string"
                        description = "${e.message}"
                        color = Colors.ERROR.color
                    }
                }
            } catch (e: SubCommandNotFoundException) {
                val bestCommand = e.command.finalArgs.maxByOrNull { it.countArgs(args) }
                val prediction = if (bestCommand != null) {
                    "\n" +
                    "Did you mean:\n" +
                        "`${Main.prefix}${e.command.name} ${bestCommand.printArgHelp()}`?\n"
                } else {
                    ""
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
                        description = "${e.message}\n" +
                            prediction +
                            "\n" +
                            "Syntax for `${Main.prefix}${e.command.name}`:\n" +
                            "\n" +
                            syntax
                        color = Colors.ERROR.color
                    }
                }
            }
        } catch (e: Exception) {
            runOldCommand(event, string)
        }
    }

    private suspend fun runOldCommand(event: MessageReceiveEvent, string: String) {
        val cmd = CmdOld(event)

        try {
            try {
                val exit = Bot.dispatcher.execute(string, cmd)
                cmd.file(event)
                if (exit != 0) MessageSendUtils.log("(executed with exit code $exit)")
            } catch (e: CommandSyntaxException) {
                if (CommandManagerOld.isCommand(string)) {
                    val usage = CommandManagerOld.getCommand(string)?.getHelpUsage()
                    cmd.event.message.channel.send {
                        embed {
                            title = "Invalid Syntax: ${Main.prefix}$string"
                            description = "${e.message}${
                                usage?.let {
                                    "\n\n$it"
                                } ?: ""
                            }"
                            color = Colors.ERROR.color
                        }
                    }
                } else if (ConfigManager.readConfigSafe<UserConfig>(
                        ConfigType.USER,
                        false
                    )?.unknownCommandError == true
                ) {
                    cmd.event.message.channel.send {
                        embed {
                            title = "Unknown Command: ${Main.prefix}${string.firstInSentence()}"
                            color = Colors.ERROR.color
                        }
                    }

                }
            }
        } catch (e: Exception) {
            event.message.error("```\n${e.stackTraceToString()}\n".flat(2045) + "```") // TODO: proper command to view stacktraces
        }
    }

}

