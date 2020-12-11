package org.kamiblue.botkt.command.commands.moderation

import kotlinx.coroutines.*
import net.ayataka.kordis.entity.botUser
import net.ayataka.kordis.entity.everyone
import net.ayataka.kordis.entity.find
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.role.Role
import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error
import java.util.concurrent.ConcurrentHashMap

object MuteCommand : BotCommand(
    name = "mute",
    alias = arrayOf("shut", "shutup", "shh"),
    category = Category.MODERATION,
    description = "Now stop talking!"
) {

    private val serverMap = HashMap<Long, ServerMuteInfo>() // <Server ID, ServerMuteInfo>

    init {
        user("user") { userArg ->
            long("duration") { durationArg ->
                string("unit") { unitArg ->
                    greedy("reason") { reasonArg ->
                        executeIfHas(PermissionTypes.COUNCIL_MEMBER) {
                            handleMute(userArg.value, durationArg.value, unitArg.value, reasonArg.value)
                        }
                    }

                    executeIfHas(PermissionTypes.COUNCIL_MEMBER) {
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
            message.error("Server members are null, are you running this from a DM?")
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

        serverMap.getOrPut(server.id) { ServerMuteInfo(server) }.mute(member, message, convertedDuration, reason)
    }

    private suspend fun ServerMuteInfo.mute(
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
        (if (long > 1) "$long ${string}s" else "$long $string") + if (appendSpace) " " else ""

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
                        "You were muted for $formattedDuration by:",
                        "${message.author?.mention ?: "Mute message author not found!"}, in the guild `${server.name}`"
                    )
                    field(
                        "Mute reason:",
                        reason
                    )
                    color = Colors.ERROR.color
                    footer("ID: ${message.author?.id}", message.author?.avatar?.url)
                }
            }
        } catch (e: Exception) {
            message.channel.send {
                embed {
                    title = "Error"
                    description = "I couldn't DM that user the ban reason, they might have had DMs disabled."
                    color = Colors.ERROR.color
                }
            }
        }

        message.channel.send {
            embed {
                field(
                    "${member.name}#${member.discriminator} was muted for $formattedDuration by:",
                    message.author?.mention ?: "Mute message author not found!"
                )
                field(
                    "Mute reason:",
                    reason
                )
                footer("ID: ${member.id}", member.avatar.url)
                color = Colors.ERROR.color
            }
        }
    }

    private suspend fun ServerMuteInfo.startUnmuteCoroutine(
        member: Member,
        role: Role,
        duration: Long
    ) {
        coroutineMap[member.id] = GlobalScope.launch {
            delay(duration)
            member.removeRole(role)
            muteMap.remove(member.id)
            coroutineMap.remove(member.id)
        }
    }

    class ServerMuteInfo(val server: Server) {
        val muteMap = ConcurrentHashMap<Long, Long>() // <Member ID, Unmute Time>
        val coroutineMap = HashMap<Long, Job>() // <Member ID, Coroutine Job>

        private var mutedRole: Role? = null

        suspend fun getMutedRole() = mutedRole
            ?: server.roles.findByName("Muted")
            ?: server.createRole {
                name = "Muted"
                permissions = PermissionSet(server.roles.everyone.permissions.compile() and 68224001)
                position = server.members.botUser.roles.map { it.position }.maxOrNull()!!
            }

        init {
            GlobalScope.launch {
                delay(10000L)
                while (isActive) {
                    for ((id, unmuteTime) in muteMap) {
                        delay(500L)
                        if (!coroutineMap.contains(id)) {
                            val member = server.members.find(id) ?: continue
                            val duration = unmuteTime - System.currentTimeMillis()
                            startUnmuteCoroutine(member, getMutedRole(), duration)
                        }
                    }
                }
            }
        }
    }

}