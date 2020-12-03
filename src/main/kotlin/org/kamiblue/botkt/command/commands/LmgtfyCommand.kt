package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.command.Command
import org.kamiblue.botkt.command.arg
import org.kamiblue.botkt.command.doesLater
import org.kamiblue.botkt.command.greedyString
import org.kamiblue.botkt.utils.Colors

object LmgtfyCommand : Command("lmgtfy") {
    init {
        greedyString("search term") {
            doesLater { context ->
                val term: String = context arg "search term"
                message.channel.send {
                    embed {
                        title = "Here you go!"
                        description = "[Solution](https://lmgtfy.com/?q=${term.replace(" ", "+")})"
                        color = Colors.PRIMARY.color
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Creates a LMGTFY link with a search term:\n" +
                "`$fullName <search term>`"
    }
}