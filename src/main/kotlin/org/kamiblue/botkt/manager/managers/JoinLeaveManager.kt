package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.event.events.server.user.UserBanEvent
import net.ayataka.kordis.event.events.server.user.UserJoinEvent
import net.ayataka.kordis.event.events.server.user.UserLeaveEvent
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.command.commands.moderation.BanCommand
import org.kamiblue.botkt.config.ServerConfigs.getConfig
import org.kamiblue.botkt.config.server.JoinLeaveConfig
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.accountAge
import org.kamiblue.botkt.utils.prettyFormat
import org.kamiblue.event.listener.asyncListener
import java.awt.Color

object JoinLeaveManager : Manager {
    private val joins = HashMap<Long, Int>()

    init {
        asyncListener<UserBanEvent> {
            val config = it.getConfig<JoinLeaveConfig>()
            val banChannel = config.banChannel

            if (banChannel != -1L) {
                val channel = it.server.channels.find(banChannel) as? TextChannel ?: return@asyncListener
                sendJoinLeave("Member Banned", config.embed, channel, it.user, Colors.WARN.color)
            }
        }

        asyncListener<UserLeaveEvent> { event ->
            if (event.server.bans().any { it.user.id == event.member.id }) return@asyncListener

            val config = event.getConfig<JoinLeaveConfig>()
            val leaveChannel = config.leaveChannel

            if (leaveChannel != -1L) {
                val channel = event.server.channels.find(leaveChannel) as? TextChannel ?: return@asyncListener
                sendJoinLeave("Member Left", config.embed, channel, event.member, Colors.ERROR.color)
            }
        }

        asyncListener<UserJoinEvent> { event ->
            val member = event.member
            val config = event.getConfig<JoinLeaveConfig>()

            joins[member.id]?.let {
                if (it > 3 && config.banRepeatedJoin) {
                    banRepeatedJoin(member)
                    return@asyncListener
                }
            }

            if (config.kickTooNew && member.accountAge() < 1) {
                kickNew(member)
                return@asyncListener
            }

            val joinChannel = config.joinChannel

            if (joinChannel != -1L) {
                val channel = event.server.channels.find(joinChannel) as? TextChannel ?: return@asyncListener
                sendJoinLeave("Member Joined", config.embed, channel, member, Colors.SUCCESS.color)
            }
        }
    }

    private suspend fun banRepeatedJoin(member: Member) {
        BanCommand.ban(
            member,
            false,
            "Automated ban due to rejoining too many times while account too new.",
            member.server,
            null
        )
        joins.remove(member.id)
    }

    private suspend fun kickNew(member: Member) {
        joins[member.id] = joins.getOrDefault(member.id, 0) + 1
        val self = Main.client.botUser

        try {
            member.getPrivateChannel().send {
                embed {
                    field(
                        "Account too new!",
                        "You were automatically kicked because your account is less than 24 hours old. Rejoin once your account is older."
                    )
                    color = Colors.ERROR.color
                    footer("ID: ${self.id}", self.avatar.url)
                }
            }
            member.kick()
        } catch (e: Exception) {
            // Ignored
        }
    }

    private suspend fun sendJoinLeave(
        msgDescription: String,
        embed: Boolean?,
        channel: TextChannel,
        user: User,
        colorIn: Color
    ) {
        if (embed == false) {
            channel.send(
                "$msgDescription\n" +
                    "**Tag:** ${user.tag}\n" +
                    "**Mention:** ${user.mention}\n" +
                    "**Account Age:** ${user.accountAge()} days\n" +
                    "**ID:** ${user.id}\n"
            )
        } else {
            channel.send {
                embed {
                    title = user.tag
                    description = msgDescription
                    color = colorIn
                    thumbnailUrl = user.avatar.url

                    field("Mention:", user.mention)
                    field("Created Account:", user.timestamp.prettyFormat())
                    field("Account Age:", user.accountAge().toString() + " days")
                    if (user is Member) field("Joined Guild:", user.joinedAt.prettyFormat())
                    footer("ID: ${user.id}", user.avatar.url)
                }
            }
        }
    }
}
