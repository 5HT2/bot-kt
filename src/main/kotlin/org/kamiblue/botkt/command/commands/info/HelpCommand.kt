package org.kamiblue.botkt.command.commands.info

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.error

object HelpCommand : BotCommand(
    name = "help",
    alias = arrayOf("command", "commands", "cmd", "cmds"),
    category = Category.INFO,
    description = "Get a list of commands or get help for a command"
) {
    init {
        int("page") { pageArg ->
            execute("Get help for specific page") {
                val index = pageArg.value - 1
                if (index in Category.values().indices) {
                    printHelpForCategory(Category.values()[index])
                } else {
                    channel.error("Invalid page number")
                }
            }
        }

        enum<Category>("category") { categoryArg ->
            execute("Get help for a specific category") {
                printHelpForCategory(categoryArg.value)
            }
        }

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
                        if (alias.isNotEmpty()) {
                            field("Aliases:", alias)
                        }
                        field("Syntax:", syntax)
                        color = Colors.PRIMARY.color
                    }
                }
            }
        }

        execute("List command category") {
            message.channel.send {
                embed {
                    title = "List of command category:"
                    description = Category.values().joinToString("\n") {
                        "**${it.ordinal + 1}.** $it"
                    }
                    color = Colors.PRIMARY.color
                }
            }
        }
    }

    private suspend fun MessageExecuteEvent.printHelpForCategory(category: Category) {
        val categoryOrder = "${category.ordinal + 1}/${Category.values().size}"

        message.channel.send {
            embed {
                title = "List of available $category commands ($categoryOrder):"
                description = category.commands.joinToString("\n") {
                    "`${Main.prefix}${it.name}` - ${it.description}"
                }
                color = Colors.PRIMARY.color
            }
        }
    }
}
