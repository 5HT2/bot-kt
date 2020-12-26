package org.kamiblue.botkt.command.commands.`fun`

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.Colors

object LmgtfyCommand : BotCommand(
    name = "lmgtfy",
    category = Category.FUN,
    description = "Creates a LMGTFY link with a search term."
) {
    init {
        greedy("search term") { termArg ->
            execute {
                message.channel.send {
                    embed {
                        title = "Here you go!"
                        description = "[Solution](https://lmgtfy.com/?q=${termArg.value.replace(" ", "+")})"
                        color = Colors.PRIMARY.color
                    }
                }
            }
        }
    }
}
