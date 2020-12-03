package org.kamiblue.botkt.command.commands

import net.ayataka.kordis.entity.findByTag
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.member.Member
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.ReactionUtils.FakeUser
import org.kamiblue.botkt.utils.SnowflakeHelper.prettyFormat
import org.kamiblue.botkt.utils.SnowflakeHelper.toInstant
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.botkt.utils.StringUtils.toUserID
import org.kamiblue.botkt.utils.authenticatedRequest
import org.kamiblue.botkt.utils.getAuthToken
import java.time.Instant
import java.time.temporal.ChronoUnit

object UserInfoCommand : Command("userinfo") {
    init {
        doesLater {
            val username: String = message.author?.id?.toString() ?: run {
                message.author?.tag ?: run {
                    message.error("Couldn't find your user, try using a direct ID!")
                    return@doesLater
                }
            }
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
                    title = it.tag
                    color = Colors.PRIMARY.color
                    thumbnailUrl = it.avatar.url

                    field("Created Account:", it.timestamp.prettyFormat())
                    field("Joined Guild:", it.joinedAt.prettyFormat())
                    field("Join Age:", it.joinedAt.until(Instant.now(), ChronoUnit.DAYS).toString() + " days")
                    field("Account Age:", it.timestamp.until(Instant.now(), ChronoUnit.DAYS).toString() + " days")
                    field("Mention:", it.mention)
                    field("ID:", "`${it.id}`")
                    field("Status:", it.status.name.toHumanReadable())
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
                    title = user.username + "#" + user.discriminator
                    color = Colors.PRIMARY.color
                    thumbnailUrl = "https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.png"

                    field("Created Account:", user.id.toInstant().prettyFormat())
                    field("Joined Guild:", current)
                    field("Join Age:", current)
                    field("Account Age:", user.id.toInstant().until(Instant.now(), ChronoUnit.DAYS).toString() + " days")
                    field("Mention:", "<@!${user.id}>")
                    field("ID:", "`${user.id}`")
                    field("Status:", current)
                }
            }
        }
    }

    private const val current = "Not in current guild!"

    override fun getHelpUsage(): String {
        return "$fullName + <user id/user tag/user name>"
    }
}
