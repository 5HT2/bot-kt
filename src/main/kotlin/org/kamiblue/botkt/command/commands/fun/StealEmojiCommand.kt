package org.kamiblue.botkt.command.commands.`fun`

import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import org.kamiblue.botkt.PermissionTypes.COUNCIL_MEMBER
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.normal
import org.kamiblue.botkt.utils.StringUtils.readBytes
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import java.io.FileNotFoundException

object StealEmojiCommand : BotCommand(
    name = "stealemoji",
    category = Category.FUN,
    description = "Emoji theif!"
) {
    init {
        emoji("emoji") { emojiArg ->
            executeIfHas(COUNCIL_MEMBER) {
                val emoji = emojiArg.value.emoji
                val extension = if (emojiArg.value.animated) "gif" else "png"
                val bytes = "https://cdn.discordapp.com/emojis/${emoji.id}.$extension".readBytes()

                steal(emoji.name, bytes, message, server)
            }
        }

        string("name") { name ->
            long("emoji id") { idArg ->
                executeIfHas(COUNCIL_MEMBER) {
                    val id = idArg.value
                    val bytes = try {
                        "https://cdn.discordapp.com/emojis/$id.png".readBytes()
                    } catch (e: FileNotFoundException) {
                        "https://cdn.discordapp.com/emojis/$id.gif".readBytes()
                    } catch (e: FileNotFoundException) {
                        message.error("Couldn't find an emoji with the ID `$id`!")
                        return@executeIfHas
                    }

                    steal(name.value, bytes, message, server)
                }
            }

            greedy("emoji url") { urlArg ->
                executeIfHas(COUNCIL_MEMBER) {
                    val idUnchecked = try {
                        urlArg.value.substring(34, 52)
                    } catch (e: StringIndexOutOfBoundsException) {
                        message.error("${urlArg.name.toHumanReadable()} is not valid format!")
                        return@executeIfHas
                    }

                    val id = idUnchecked.toLongOrNull() ?: run {
                        message.error("Emoji ID `$idUnchecked` could not be formatted to a Long!")
                        return@executeIfHas
                    }

                    val bytes = try {
                        "https://cdn.discordapp.com/emojis/$id.png".readBytes()
                    } catch (e: FileNotFoundException) {
                        "https://cdn.discordapp.com/emojis/$id.gif".readBytes()
                    } catch (e: FileNotFoundException) {
                        message.error("Couldn't find an emoji with the ID `$id`!")
                        return@executeIfHas
                    }

                    steal(name.value, bytes, message, server)
                }
            }
        }
    }

    private suspend fun steal(emojiName: String, emojiImage: ByteArray, message: Message, server: Server?) {
        val foundEmoji = server?.emojis?.findByName(emojiName)
        if (foundEmoji != null) {
            message.error("There is already an emoji with the name `$emojiName`!")
            return
        }

        val emoji = server?.createEmoji {
            name = emojiName
            image = emojiImage
        } ?: run {
            message.error("Guild is null, make sure you're not running this from a DM!")
            return
        }

        message.normal("Successfully stolen emoji `${emoji.name}`!")
    }
}