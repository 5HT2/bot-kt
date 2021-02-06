package org.kamiblue.botkt.command.commands.`fun`

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.Colors

object LmddgtfyCommand : BotCommand(
    name = "lmddgtfy",
    category = Category.FUN,
    description = "Creates a LMDDGTFY link with a search term."
) {
    init {
        greedy("search term") { termArg ->
            execute {
                message.channel.send {
                    embed {
                        title = "Here you go!"
                        description = "[Solution](https://lmddgtfy.net/?q=${termArg.value.replace(" ", "+")})"
                        color = Colors.PRIMARY.color
                    }
                }
            }
        }
    }
}
