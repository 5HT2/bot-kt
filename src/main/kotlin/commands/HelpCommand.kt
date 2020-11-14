package org.kamiblue.botkt.commands

import org.kamiblue.botkt.*

object HelpCommand : Command("help-mod") {
    init {
        string("command") {
            doesLater { context ->
                val userCommand: String = context arg "command"
                if (CommandManager.isCommand(userCommand)) {
                    val command = CommandManager.getCommandClass(userCommand)!!
                    message.channel.send {
                        embed {
                            title = userCommand.toLowerCase()
                            description = command.getHelpUsage()
                            color = Colors.primary
                        }
                    }
                } else {
                    message.channel.send {
                        embed {
                            title = "Error"
                            description = "Command `$userCommand` not found!"
                            color = Colors.error
                        }
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Gives usage examples for commands.\n\n" +
                "Usage:\n" +
                "`$fullName <command name>`"
    }
}