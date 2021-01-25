package org.kamiblue.botkt.command.commands.moderation

import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay
import net.ayataka.kordis.DiscordClientImpl
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.permission.Permission
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.exception.NotFoundException
import org.kamiblue.botkt.*
import org.kamiblue.botkt.PermissionTypes.COUNCIL_MEMBER
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.manager.managers.ConfigManager.readConfigSafe
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.checkPermission
import org.kamiblue.botkt.utils.error
import org.kamiblue.botkt.utils.normal
import org.kamiblue.commons.extension.max

object BanCommand : BotCommand(
    name = "ban",
    category = Category.MODERATION,
    description = "Ban a user or multiple users"
) {
    private const val banReason = "Ban Reason:"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        literal("regex") {
            literal("confirm") {
                greedy("userRegex") { userRegexArg ->
                    executeIfHas(PermissionTypes.MASS_BAN, "Mass ban members by regex") {
                        val server = server ?: run { channel.error("Server members are null, are you running this from a DM?"); return@executeIfHas }

                        val m = channel.error("Banning [calculating] members...")

                        var banned = 0
                        val regex = userRegexArg.value.toRegex()
                        val reason = "Mass ban by ${message.author?.mention}"
                        val filtered = server.members.filter { it.name.contains(regex) }

                        if (filtered.isEmpty()) {
                            m.edit {
                                description = "Not banning anybody! 0 members found."
                                color = Colors.ERROR.color
                            }
                            return@executeIfHas
                        } else {
                            m.edit {
                                description = "Banning ${filtered.size} members..."
                                color = Colors.ERROR.color
                            }
                        }

                        filtered.forEach {
                            banned++
                            ban(it, true, reason, server, null)
                            delay(200)
                        }

                        m.edit {
                            field(
                                "$banned members were banned by:",
                                message.author?.mention.toString()
                            )
                            field(
                                banReason,
                                reason
                            )
                            footer("ID: ${message.author?.id}", message.author?.avatar?.url)
                            color = Colors.ERROR.color
                        }
                    }
                }
            }

            greedy("userRegex") { userRegexArg ->
                executeIfHas(PermissionTypes.MASS_BAN, "Preview mass banning by regex") {
                    val regex = userRegexArg.value.toRegex()

                    val members = server?.members ?: run {
                        channel.error("Server members are null, are you running this from a DM?")
                        return@executeIfHas
                    }

                    val filtered = members.filter { it.name.contains(regex) }.joinToString(separator = "\n") { it.mention }

                    if (members.isEmpty()) {
                        channel.error("Couldn't find any members that match the regex `$regex`!")
                    } else {
                        channel.normal(filtered.max(2048, "\nNot all users are shown, due to size limitations."))
                    }
                }
            }
        }

        user("user") { user ->
            literal("purge") {
                greedy("reason") { reason ->
                    executeIfHas(COUNCIL_MEMBER, "Delete messages, custom reason") {
                        ban(user.value, true, reason.value, server, message)
                    }
                }
            }

            greedy("reason") { reason ->
                executeIfHas(COUNCIL_MEMBER, "Don't delete messages, custom reason") {
                    ban(user.value, false, reason.value, server, message)
                }
            }

            executeIfHas(COUNCIL_MEMBER, "Don't delete messages, use default reason") {
                ban(user.value, false, null, server, message)
            }
        }
    }

    suspend fun ban(
        user: User,
        deleteMsgs: Boolean, // if we should delete the past day of their messages or not
        reason: String?, // reason why they were banned. tries to dm before banning
        nullableServer: Server?,
        message: Message?
    ) {
        val server = nullableServer ?: run { message?.channel?.error("Server is null, make sure you aren't running this from a DM!"); return }

        val deleteMessageDays = if (deleteMsgs) 1 else 0
        val fixedReason = if (!reason.isNullOrBlank()) reason else readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultBanReason ?: "No Reason Specified"

        if (!canBan(user, message, server)) return

        messageReason(user, message, server, fixedReason)

        try {
            user.ban(
                server,
                deleteMessageDays,
                fixedReason
            )
        } catch (e: Exception) {
            message?.channel?.send {
                embed {
                    title = "Error"
                    description = "That user's role is higher then mine, I can't ban them!"
                    field("Stacktrace:", "```${e.message}\n${e.stackTraceToString().max(256)}```")
                    color = Colors.ERROR.color
                }
            }
            return
        }

        message?.let { msg ->
            msg.channel.send {
                embed {
                    field(
                        "${user.tag} was banned by:",
                        msg.author?.mention ?: "Ban message author not found!"
                    )
                    field(
                        banReason,
                        fixedReason
                    )
                    footer("ID: ${user.id}", user.avatar.url)
                    color = Colors.ERROR.color
                }
            }
        }
    }

    private suspend fun messageReason(bannedUser: User, message: Message?, server: Server, fixedReason: String) {
        val user = message?.author ?: Main.client.botUser
        try {
            bannedUser.getPrivateChannel().send {
                embed {
                    field(
                        "You were banned by:",
                        user.mention
                    )
                    field(
                        "In the guild:",
                        server.name
                    )
                    field(
                        banReason,
                        fixedReason
                    )
                    color = Colors.ERROR.color
                    footer("ID: ${user.id}", user.avatar.url)
                }
            }
        } catch (e: Exception) {
            message?.channel?.send {
                embed {
                    title = "Error"
                    description = "I couldn't DM that user the ban reason, they might have had DMs disabled."
                    color = Colors.ERROR.color
                }
            }
        }
    }

    private suspend fun canBan(user: User, message: Message?, server: Server): Boolean {
        when {
            user.hasPermission(COUNCIL_MEMBER) -> {
                message?.channel?.error("That user is protected, I can't do that.")
                return false
            }

            user.id == message?.author?.id -> {
                message.channel.error("You can't ban yourself!")
                return false
            }

            else -> {
                try {
                    checkPermission(Main.client as DiscordClientImpl, server, Permission.BAN_MEMBERS)
                } catch (e: NotFoundException) {
                    message?.channel?.error("Client is not fully initialized, member list not loaded!")
                    return false
                }
                return true
            }
        }
    }
}
