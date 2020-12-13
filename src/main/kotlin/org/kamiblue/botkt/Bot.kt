package org.kamiblue.botkt

import kotlinx.coroutines.delay
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.server.enums.ActivityType
import net.ayataka.kordis.entity.server.enums.UserStatus
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import net.ayataka.kordis.event.events.server.user.UserBanEvent
import net.ayataka.kordis.event.events.server.user.UserJoinEvent
import net.ayataka.kordis.event.events.server.user.UserLeaveEvent
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.command.commands.moderation.BanCommand
import org.kamiblue.botkt.event.KordisEventProcessor
import org.kamiblue.botkt.helpers.UpdateHelper
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils
import org.kamiblue.botkt.utils.SnowflakeHelper.prettyFormat
import org.kamiblue.botkt.utils.accountAge

/**
 * @author l1ving
 * @since 16/08/20 17:30
 */
object Bot {

    suspend fun start() {
        val started = System.currentTimeMillis()

        MessageSendUtils.log("Starting bot!")

        UpdateHelper.writeVersion(Main.currentVersion)
        UpdateHelper.updateCheck()

        val config = ConfigManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            MessageSendUtils.log("Bot token not found, make sure your file is formatted correctly!. \nExiting...")
            Main.exit()
            return
        }

        Main.client = Kordis.create {
            token = config.botToken
            // Annotation based Event Listener
            addListener(this@Bot)
        }

        CommandManager.init()

        val initMessage = "Initialized bot!\n" +
            "Running on ${Main.currentVersion}\n" +
            "Startup took ${System.currentTimeMillis() - started}ms"

        val userConfig = ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)

        updateStatus(userConfig)

        delay(2000) // Discord API is really stupid and doesn't give you the information you need right away, hence delay needed

        sendStartupMessage(userConfig, initMessage)

        Main.client.addListener(KordisEventProcessor)
        Main.ready = true
        MessageSendUtils.log(initMessage)
    }

    private fun updateStatus(userConfig: UserConfig?) {
        userConfig?.statusMessage?.let { message ->
            val type = userConfig.statusMessageType?.let {
                ActivityType.values().getOrNull(it)
            } ?: ActivityType.UNKNOWN

            Main.client.updateStatus(UserStatus.ONLINE, type, message)
        }
    }

    private suspend fun sendStartupMessage(userConfig: UserConfig?, initMessage: String) {
        userConfig?.startUpChannel?.let {
            if (userConfig.primaryServerId == null) {
                Main.client.servers.forEach { server ->
                    delay(100) // we don't want to hit the message rate limit, 10 messages a second should be fine
                    server.textChannels.findByName(it)?.send {
                        embed {
                            title = "Startup"
                            description = initMessage
                            color = Colors.SUCCESS.color
                        }
                    }
                }
            } else {
                val channel = Main.client.servers.find(userConfig.primaryServerId)?.textChannels?.findByName(it)
                channel?.send {
                    embed {
                        title = "Startup"
                        description = initMessage
                        color = Colors.SUCCESS.color
                    }
                }
            }
        }
    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
        if (!Main.ready || event.message.content.isEmpty()) return // message can be empty on images, embeds and other attachments

        val string = if (event.message.content[0] == Main.prefix) event.message.content.substring(1) else return

        CommandManager.submit(event, string)
    }

    private val joins = HashMap<Long, Int>()

    @EventHandler
    suspend fun onMemberBan(event: UserBanEvent) {
        if (!Main.ready) return

        val cfg = ConfigManager.readConfigSafe<JoinLeaveConfig>(ConfigType.JOIN_LEAVE, false)

        cfg?.banChannel?.let { banChannel ->
            val channel = event.server.channels.find(banChannel) as? TextChannel ?: return
            sendJoinLeave("Member Banned", cfg.embed, channel, event.user)
        }
    }

    @EventHandler
    suspend fun onMemberLeave(event: UserLeaveEvent) {
        if (!Main.ready) return

        if (event.member.server.bans().any { it.user.id == event.member.id }) return
        val cfg = ConfigManager.readConfigSafe<JoinLeaveConfig>(ConfigType.JOIN_LEAVE, false)

        cfg?.leaveChannel?.let { leaveChannel ->
            val channel = event.server.channels.find(leaveChannel) as? TextChannel ?: return
            sendJoinLeave("Member Left", cfg.embed, channel, event.member, event.member)
        }
    }

    @EventHandler
    suspend fun onMemberJoin(event: UserJoinEvent) {
        if (!Main.ready) return

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
                return
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
            return
        }

        cfg?.joinChannel?.let { joinChannel ->
            val channel = event.server.channels.find(joinChannel) as? TextChannel ?: return
            sendJoinLeave("Member Joined", cfg.embed, channel, member, member)
        }
    }

    private suspend fun sendJoinLeave(msgDescription: String, embed: Boolean?, channel: TextChannel, user: User, member: Member? = null) {
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

