package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.event.events.server.user.UserBanEvent
import net.ayataka.kordis.event.events.server.user.UserJoinEvent
import net.ayataka.kordis.event.events.server.user.UserLeaveEvent
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.JoinLeaveConfig
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.command.commands.moderation.BanCommand
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.SnowflakeHelper.prettyFormat
import org.kamiblue.botkt.utils.accountAge
import org.kamiblue.event.listener.asyncListener

object JoinLeaveManager : Manager {
    private val joins = HashMap<Long, Int>()

    init {
        asyncListener<UserBanEvent> {
            val cfg = ConfigManager.readConfigSafe<JoinLeaveConfig>(ConfigType.JOIN_LEAVE, false)

            cfg?.banChannel?.let { banChannel ->
                val channel = it.server.channels.find(banChannel) as? TextChannel ?: return@asyncListener
                sendJoinLeave("Member Banned", cfg.embed, channel, it.user)
            }
        }

        asyncListener<UserLeaveEvent> { event ->
            if (event.member.server.bans().any { it.user.id == event.member.id }) return@asyncListener
            val cfg = ConfigManager.readConfigSafe<JoinLeaveConfig>(ConfigType.JOIN_LEAVE, false)

            cfg?.leaveChannel?.let { leaveChannel ->
                val channel = event.server.channels.find(leaveChannel) as? TextChannel ?: return@asyncListener
                sendJoinLeave("Member Left", cfg.embed, channel, event.member, event.member)
            }
        }

        asyncListener<UserJoinEvent> { event ->
            val cfg = ConfigManager.readConfigSafe<JoinLeaveConfig>(ConfigType.JOIN_LEAVE, false)
            val member = event.member
            val self = Main.client.botUser

            joins[member.id]?.let {
                if (it > 3 && cfg?.banRepeatedJoin == true) {
                    BanCommand.ban(
                        member,
                        false,
                        "Automated ban due to rejoining too many times while account too new.",
                        member.server,
                        null
                    )
                    joins.remove(member.id)
                    return@asyncListener
                }
            }

            if (cfg?.kickTooNew == true && member.accountAge() < 1) {
                joins[member.id] = joins.getOrDefault(member.id, 0) + 1
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
                    // this is fine
                }
                return@asyncListener
            }

            cfg?.joinChannel?.let { joinChannel ->
                val channel = event.server.channels.find(joinChannel) as? TextChannel ?: return@asyncListener
                sendJoinLeave("Member Joined", cfg.embed, channel, member, member)
            }
        }
    }

    private suspend fun sendJoinLeave(
        msgDescription: String,
        embed: Boolean?,
        channel: TextChannel,
        user: User,
        member: Member? = null
    ) {
        if (embed == false) {
            channel.send(
                "$msgDescription\n" +
                    "**Tag:** ${user.tag}\n" +
                    "**Mention:** ${user.mention}\n" +
                    "**ID:** ${user.id}\n" +
                    "**Account Age:** ${user.accountAge()} days\n"
            )
        } else {
            channel.send {
                embed {
                    title = user.tag
                    description = msgDescription
                    color = Colors.PRIMARY.color
                    thumbnailUrl = user.avatar.url

                    field("Created Account:", user.timestamp.prettyFormat())
                    member?.let { field("Joined Guild:", it.joinedAt.prettyFormat()) }
                    field("Account Age:", user.accountAge().toString() + " days")
                    field("Mention:", user.mention)
                    field("ID:", "`${user.id}`")
                }
            }
        }
    }
}