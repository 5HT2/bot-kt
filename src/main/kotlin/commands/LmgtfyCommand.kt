package commands

import Command
import arg
import doesLater
import greedyString

object LmgtfyCommand : Command("lmgtfy") {
    init {
        greedyString("search term") {
            doesLater { context ->
                val term: String = context arg "search term"
                message.channel.send {
                    embed {
                        title = "Here you go!"
                        description = "[Solution](https://lmgtfy.com/?q=${term.replace(" ", "+")})"
                        color = Main.Colors.BLUE.color
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Creates a LMGTFY link with a search term:\n" +
                "`;$name <search term>`"
    }
}