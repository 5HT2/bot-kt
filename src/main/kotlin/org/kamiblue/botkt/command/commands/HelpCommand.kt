package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.Colors

object HelpCommand : CommandOld("help-mod") {
    init {
        string("command") {
            doesLater { context ->
                val userCommand: String = context arg "command"
                if (CommandManagerOld.isCommand(userCommand)) {
                    val command = CommandManagerOld.getCommand(userCommand)!!
                    message.channel.send {
                        embed {
                            title = userCommand.toLowerCase()
                            description = command.getHelpUsage()
                            color = Colors.PRIMARY.color
                        }
                    }
                } else {
                    message.channel.send {
                        embed {
                            title = "Error"
                            description = "Command `$userCommand` not found!"
                            color = Colors.ERROR.color
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