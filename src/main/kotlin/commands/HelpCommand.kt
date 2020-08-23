package commands

import Command
import CommandManager
import Main.Colors.BLUE
import Main.Colors.ERROR
import arg
import doesLater
import string

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
                            color = BLUE.color
                        }
                    }
                } else {
                    message.channel.send {
                        embed {
                            title = "Error"
                            description = "Command `$userCommand` not found!"
                            color = ERROR.color
                        }
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Gives usage examples for commands.\n\n" +
                "Usage:\n" +
                "`;$name <command name>`"
    }
}