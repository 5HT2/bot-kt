package org.kamiblue.botkt.commands

import net.ayataka.kordis.entity.findByTag
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.member.Member
import org.kamiblue.botkt.Command
import org.kamiblue.botkt.arg
import org.kamiblue.botkt.doesLater
import org.kamiblue.botkt.greedyString
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.ReactionUtils.FakeUser
import org.kamiblue.botkt.utils.SnowflakeHelper.prettyFormat
import org.kamiblue.botkt.utils.SnowflakeHelper.toInstant
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.botkt.utils.StringUtils.toUserID
import org.kamiblue.botkt.utils.authenticatedRequest
import org.kamiblue.botkt.utils.getAuthToken

object UserInfoCommand : Command("userinfo") {
    init {
        doesLater {
            val username: String = message.author?.id?.toString() ?: run { message.author?.tag ?: run {
                message.error("Couldn't find your user, try using a direct ID!")
                return@doesLater
            }}
            send(username, message)
        }

        greedyString("name") {
            doesLater { context ->
                val username: String = context arg "name"
                send(username, message)
            }
        }
    }

    private suspend fun send(username: String, message: Message) {
        val member: Member? = username.toUserID()?.let {
            message.server?.members?.find(it)
        } ?: message.server?.members?.findByTag(username)
        ?: message.server?.members?.findByName(username)

        member?.let {
            message.channel.send {
                embed {
                    title = it.nickname ?: it.name
                    color = Colors.PRIMARY.color
                    thumbnailUrl = it.avatar.url

                    field("Created Account:", it.timestamp.prettyFormat(), true)
                    field("Joined Guild:", it.joinedAt.prettyFormat(), true)
                    field("Mention:", it.mention, true)
                    field("Tag:", it.tag, true)
                    field("ID:", "`${it.id}`", true)
                    field("Status:", it.status.name.toHumanReadable(), true)
                }
            }

        } ?: run {
            val id = username.toUserID() ?: run {
                message.error("Couldn't find user nor a valid ID!")
                return
            }

            val user = authenticatedRequest<FakeUser>(
                "Bot",
                getAuthToken(),
                "https://discord.com/api/v8/users/$id"
            )

            message.channel.send {
                embed {
                    title = user.username
                    color = Colors.PRIMARY.color
                    thumbnailUrl = "https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.png"

                    field("Created:", user.id.toInstant().prettyFormat(), true)
                    field("Joined:", "Not in current guild!", true)
                    field("Mention:", "<@!${user.id}>", true)
                    field("Tag:", "${user.username}#${user.discriminator}", true)
                    field("Status:", "Not in current guild!", true)
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "$fullName + <user id/user tag/user name>"
    }
}
