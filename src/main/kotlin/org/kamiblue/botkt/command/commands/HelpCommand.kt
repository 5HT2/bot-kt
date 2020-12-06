package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.utils.Colors

object HelpCommand : BotCommand(
    name = "help-mod",
    alias = arrayOf("command", "commands", "cmd", "cmds"),
    description = "Get a list of commands or get help for a command"
) {
    init {
        string("command name") { commandNameArg ->
            execute {
                val command = commandManager.getCommand(commandNameArg.value)
                val alias = command.alias.joinToString()
                val syntax = command.printArgHelp()
                    .lines()
                    .joinToString("\n") {
                        if (it.isNotBlank() && !it.startsWith("    - ")) {
                            "`${Main.prefix}${command.name} $it`"
                        } else {
                            it
                        }
                    }

                message.channel.send {
                    embed {
                        title = "Help for `${Main.prefix}${command.name}`"
                        field("alias:", alias)
                        field("description:", command.description)
                        field("syntax:", syntax)
                        color = Colors.PRIMARY.color
                    }
                }
            }
        }

        execute {
            message.channel.send {
                embed {
                    title = "List of available commands:"
                    description = commandManager.getCommands().joinToString("\n") {
                        "`${Main.prefix}${it.name}` - ${it.description}"
                    }
                    color = Colors.PRIMARY.color
                }
            }
        }
    }
}
