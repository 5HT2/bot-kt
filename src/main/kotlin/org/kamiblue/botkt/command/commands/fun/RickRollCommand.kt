package org.kamiblue.botkt.command.commands.`fun`

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.MessageUtils.error
import org.kamiblue.botkt.utils.StringUtils.isUrl

object RickRollCommand : BotCommand(
    name = "rickroll",
    alias = arrayOf("rick", "rickastley"),
    category = Category.FUN,
    description = "Never gonna give you up, Never gonna let you down..."
) {
    private val cdnRegex = "https://cdn.discordapp.com/attachments/(\\d{18})/(\\d{18})/(.*\$)".toRegex()

    init {
        string("url") { urlArg ->
            execute {
                val url = urlArg.value
                if (!url.isUrl()) {
                    message.channel.error("Invalid url!")
                    return@execute
                }
                val redirectLink = url.replace(cdnRegex, "https://cdn.dircordapp.com/attachments/$1/$2/$3")
                message.delete()
                message.channel.send(redirectLink)
            }
        }
    }

}