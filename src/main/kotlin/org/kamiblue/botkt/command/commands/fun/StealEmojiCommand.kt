package org.kamiblue.botkt.command.commands.`fun`

import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.normal
import org.kamiblue.botkt.utils.StringUtils.readBytes

object StealEmojiCommand : BotCommand(
    name = "stealemoji",
    category = Category.FUN,
    description = "Emoji theif!"
) {
    init {
        emoji("emoji") { emojiArg ->
            executeIfHas(PermissionTypes.COUNCIL_MEMBER) {
                val animatableEmoji = emojiArg.value

                val foundEmoji = server?.emojis?.findByName(animatableEmoji.emoji.name)
                if (foundEmoji != null) {
                    message.error("There is already an emoji with the name `${animatableEmoji.emoji.name}`!")
                    return@executeIfHas
                }

                val extension = if (animatableEmoji.animated) "gif" else "png"
                val bytes = "https://cdn.discordapp.com/emojis/${animatableEmoji.emoji.id}.$extension".readBytes()

                val emoji = server?.createEmoji {
                    name = animatableEmoji.emoji.name
                    image = bytes
                } ?: run {
                    message.error("Guild is null, make sure you're not running this from a DM!")
                    return@executeIfHas
                }

                message.normal("Successfully stolen emoji `${emoji.name}`!")
            }

        }
    }
}