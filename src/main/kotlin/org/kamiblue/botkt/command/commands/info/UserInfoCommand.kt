package org.kamiblue.botkt.command.commands.info

import net.ayataka.kordis.entity.findByTag
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.member.Member
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.*
import org.kamiblue.botkt.utils.MessageUtils.error
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.botkt.utils.StringUtils.toUserID

object UserInfoCommand : BotCommand(
    name = "userinfo",
    alias = arrayOf("whois"),
    category = Category.INFO,
    description = "Look up info for a Discord user"
) {

    private const val current = "Not in current guild!"

    init {
        execute("Get info for yourself") {
            val username: String = message.author?.id?.toString() ?: run {
                message.author?.tag ?: run {
                    message.channel.error("Couldn't find your user, try using a direct ID!")
                    return@execute
                }
            }
            send(username, message)
        }

        greedy("name") { nameArg ->
            execute("Find a user with their name, a ping or their ID") {
                val name = nameArg.value
                send(name, message)
            }
        }
    }

    private suspend fun send(username: String, message: Message) {
        val user = username.toUserID()?.let { message.server?.members?.find(it) }
            ?: message.server?.members?.findByTag(username)
            ?: message.server?.members?.findByName(username)
            ?: run {
                val id = username.toUserID() ?: run {
                    message.channel.error("Couldn't find user nor a valid ID!")
                    return
                }
                requestUser(id)
            }

        message.channel.send {
            embed {
                title = user.tag
                color = Colors.PRIMARY.color
                thumbnailUrl = user.avatar.url

                field("Created Account:", user.timestamp.prettyFormat())
                field("Joined Guild:", if (user is Member) user.joinedAt.prettyFormat() else current)
                field("Join Age:", if (user is Member) "${user.joinedAt.untilNow()} days" else current)
                field("Account Age:", "${user.accountAge()} days")
                field("Mention:", user.mention)
                field("ID:", "`${user.id}`")
                field("Status:", if (user is Member) user.status.name.toHumanReadable() else current)
            }
        }
    }

}
