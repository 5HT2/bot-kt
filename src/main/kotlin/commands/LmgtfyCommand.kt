package org.kamiblue.botkt.commands

import org.kamiblue.botkt.*

object LmgtfyCommand : Command("lmgtfy") {
    init {
        greedyString("search term") {
            doesLater { context ->
                val term: String = context arg "search term"
                message.channel.send {
                    embed {
                        title = "Here you go!"
                        description = "[Solution](https://lmgtfy.com/?q=${term.replace(" ", "+")})"
                        color = Colors.primary
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