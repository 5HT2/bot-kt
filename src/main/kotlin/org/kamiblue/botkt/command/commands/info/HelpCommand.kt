package org.kamiblue.botkt.command.commands.info

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.Colors

object HelpCommand : BotCommand(
    name = "help",
    alias = arrayOf("command", "commands", "cmd", "cmds"),
    category = Category.INFO,
    description = "Get a list of commands or get help for a command"
) {
    init {
        string("command name") { commandNameArg ->
            execute("Get help for a specific command") {
                val command = commandManager.getCommand(commandNameArg.value)
                val alias = command.alias.joinToString()
                val syntax = command.printArgHelp()
                    .lines()
                    .joinToString("\n") {
                        if (it.isNotBlank() && !it.startsWith("- ")) {
                            "`${Main.prefix}$it`"
                        } else {
                            it
                        }
                    }

                message.channel.send {
                    embed {
                        title = "Help for `${Main.prefix}${command.name}`"
                        field("Description:", command.description)
                        field("Aliases:", if (alias.isEmpty()) "No aliases" else alias)
                        field("Syntax:", syntax)
                        color = Colors.PRIMARY.color
                    }
                }
            }
        }

        execute("List available commands") {
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
