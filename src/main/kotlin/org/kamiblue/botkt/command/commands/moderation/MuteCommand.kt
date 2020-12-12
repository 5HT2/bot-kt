package org.kamiblue.botkt.command.commands.moderation

import net.ayataka.kordis.entity.find
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.MuteManager
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.success

object MuteCommand : BotCommand(
    name = "mute",
    alias = arrayOf("shut", "shutup", "shh"),
    category = Category.MODERATION,
    description = "Now stop talking!"
) {

    init {
        try {
            MuteManager.load()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        literal("reload", "Reload mute config") {
            executeIfHas(PermissionTypes.MANAGE_CONFIG) {
                MuteManager.load()
                message.success("Successfully reloaded mute config!")
            }
        }

        literal("save", "Force save mute config") {
            executeIfHas(PermissionTypes.MANAGE_CONFIG) {
                MuteManager.save()
                message.success("Successfully saved mute config!")
            }
        }

        user("user") { userArg ->
            long("duration") { durationArg ->
                string("unit") { unitArg ->
                    greedy("reason") { reasonArg ->
                        executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Mute user with reason") {
                            handleMute(userArg.value, durationArg.value, unitArg.value, reasonArg.value)
                        }
                    }

                    executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Mute user without reason") {
                        handleMute(userArg.value, durationArg.value, unitArg.value, "No reason provided")
                    }
                }
            }
        }
    }

    private suspend fun MessageExecuteEvent.handleMute(
        user: User,
        duration: Long,
        unit: String,
        reason: String
    ) {
        if (server == null) {
            message.error("Server is null, are you running this from a DM?")
            return
        }

        if (user.id.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
            message.error("That user is protected, I can't do that.")
            return
        }

        if (MuteManager.serverMap[server.id]?.muteMap?.containsKey(user.id) == true) {
            message.error("${user.mention} is already muted")
            return
        }

        val convertedDuration = when (unit.toLowerCase()) {
            "s" -> duration * 1000L
            "m" -> duration * 60000L
            "h" -> duration * 3600000L
            "d" -> duration * 86400000L
            else -> {
                message.error("Invalid time unit input: $unit")
                return
            }
        }

        if (convertedDuration !in 1000L..2592000000L) {
            message.error("Duration must be at least 1 second and not longer than 1 month!")
        }

        val member = server.members.find(user) ?: run {
            message.error("Member not found!")
            return
        }

        MuteManager.serverMap.getOrPut(server.id) { MuteManager.ServerMuteInfo(server) }
            .mute(member, message, convertedDuration, reason)
    }

    private suspend fun MuteManager.ServerMuteInfo.mute(
        member: Member,
        message: Message,
        duration: Long,
        reason: String
    ) {
        val mutedRole = getMutedRole()

        try {
            member.addRole(mutedRole)
        } catch (e: Exception) {
            message.channel.send {
                embed {
                    title = "Unable to mute ${member.mention}"
                    description = e.message
                    color = Colors.ERROR.color
                }
            }
            return
        }

        val formattedDuration = formatDuration(duration)
        muteMap[member.id] = System.currentTimeMillis() + duration
        sendMutedMessage(member, message, server, formattedDuration, reason)
        startUnmuteCoroutine(member, mutedRole, duration)
    }

    private fun formatDuration(duration: Long): String {
        val day = duration / 86400000L
        val hour = duration / 3600000L % 24L
        val minute = duration / 60000L % 60L
        val second = duration / 1000L % 60L

        return StringBuilder(4).apply {
            var added = false

            if (added || day != 0L) {
                append(grammar(day, "day"))
                added = true
            }

            if (added || hour != 0L) {
                append(grammar(hour, "hour"))
                added = true
            }

            if (added || minute != 0L) {
                append(grammar(minute, "minute"))
            }

            append(grammar(second, "second", false))
        }.toString()
    }

    private fun grammar(long: Long, string: String, appendSpace: Boolean = true) =
        (if (long > 1 || long == 0L) "$long ${string}s" else "$long $string") + if (appendSpace) " " else ""

    private suspend fun sendMutedMessage(
        member: Member,
        message: Message,
        server: Server,
        formattedDuration: String,
        reason: String
    ) {
        try {
            member.getPrivateChannel().send {
                embed {
                    field(
                        "You were muted by:",
                        message.author?.mention ?: "Mute message author not found!"
                    )
                    field(
                        "Mute reason:",
                        reason
                    )
                    field(
                        "Duration:",
                        formattedDuration
                    )
                    field(
                        "In the guild:",
                        server.name
                    )
                    color = Colors.ERROR.color
                    footer("ID: ${message.author?.id}", message.author?.avatar?.url)
                }
            }
        } catch (e: Exception) {
            message.channel.send {
                embed {
                    title = "Error"
                    description = "I couldn't DM that user the mute reason, they might have had DMs disabled."
                    color = Colors.ERROR.color
                }
            }
        }

        message.channel.send {
            embed {
                field(
                    "${member.tag} was muted by:",
                    message.author?.mention ?: "Mute message author not found!"
                )
                field(
                    "Mute reason:",
                    reason
                )
                field(
                    "Duration:",
                    formattedDuration
                )
                footer("ID: ${member.id}", member.avatar.url)
                color = Colors.ERROR.color
            }
        }
    }

}