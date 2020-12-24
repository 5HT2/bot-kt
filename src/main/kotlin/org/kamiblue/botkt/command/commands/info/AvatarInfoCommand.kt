package org.kamiblue.botkt.command.commands.info

import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.error

object AvatarInfoCommand : BotCommand(
    name = "avatarinfo",
    alias = arrayOf("avatar", "av"),
    description = "Show a users avatar",
    category = Category.INFO
) {
    init {
        literal("full") {
            user("user") { userArg ->
                execute("Send the link to the users avatar") {
                    sendAvatar(userArg.value, true)
                }
            }

            execute("Send the link to your avatar") {
                sendAvatar(message.author, true)
            }
        }

        execute("Send your avatar in an embed") {
            sendAvatar(message.author)
        }

        user("user") { userArg ->
            execute("Send the users avatar in an embed") {
                sendAvatar(userArg.value)
            }
        }
    }

    private suspend fun MessageExecuteEvent.sendAvatar(user: User?, full: Boolean = false) {
        user ?: run {
            channel.error("Mentioned user was null!")
            return
        }

        if (full) {
            channel.send("${user.avatar.url}?size=2048")
        } else {
            channel.send {
                embed {
                    title = user.tag
                    imageUrl = "${user.avatar.url}?size=2048"
                    color = Colors.PRIMARY.color
                }
            }
        }
    }
}
