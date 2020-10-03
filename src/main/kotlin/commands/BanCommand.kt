package commands

import Colors
import Command
import ConfigManager.readConfigSafe
import ConfigType
import PermissionTypes
import Permissions.hasPermission
import Send.error
import UserConfig
import arg
import bool
import doesLater
import greedyString
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.user.User
import string

object BanCommand : Command("ban") {
    init {
        string("username") {
            doesLater { context ->
                if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                    return@doesLater
                }
                val serverL = server ?: run { message.error(serverError); return@doesLater }
                val username: String = context arg "username"

                ban(username, serverL, false, "", message)
            }

            greedyString("reason") {
                doesLater { context ->
                    if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                        return@doesLater
                    }
                    val serverL = server ?: run { message.error(serverError); return@doesLater }
                    val username: String = context arg "username"
                    val reason: String = context arg "reason"

                    ban(username, serverL, false, reason, message)
                }
            }

            bool("deleteMsgs") {
                greedyString("reason") {
                    doesLater { context ->
                        if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                            return@doesLater
                        }
                        val serverL = server ?: run { message.error(serverError); return@doesLater }
                        val username: String = context arg "username"
                        val deleteMsgs: Boolean = context arg "deleteMsgs"
                        val reason: String = context arg "reason"

                        ban(username, serverL, deleteMsgs, reason, message)
                    }
                }
            }
        }
    }

    private const val serverError = "Server is null, make sure you aren't running this from a DM!"

    private suspend fun ban(
        unfilteredUsername: String, // this can be an @mention (<@id>), an ID (id), or a username (username#discrim)
        server: Server,
        deleteMsgs: Boolean, // if we should delete the past day of their messages or not
        reason: String, // reason why they were banned. dmed before banning
        message: Message
    ) {

        var username = unfilteredUsername
        try {
            val filtered = username.replace("[<@!>]".toRegex(), "").toLong()
            username = filtered.toString()
        } catch (ignored: NumberFormatException) {
            // this is fine, we're parsing user input and don't know if it's an ID or not
        }

        val user: User = server.members.findByName(username) ?: server.members.find(username.toLong()) ?: // ID, or ping with the regex [<@!>] removed
        run {
            val nor: String = if (username != unfilteredUsername) "$unfilteredUsername nor $username" else unfilteredUsername
            message.error("User $nor not found!")
            return
        }

        val deleteMessageDays = if (deleteMsgs) 1 else 0

        val fixedReason = if (reason.isNotEmpty()) reason else readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultBanReason ?: "No Reason Specified"

        if (user.id == message.author?.id) {
            message.error("You can't ban yourself!")
            return
        } else if (hasPermission(user.id, PermissionTypes.COUNCIL_MEMBER)) {
            message.error("That user is protected, I can't do that.")
            return
        }

        user.getPrivateChannel().send {
            embed {
                field(
                    "You were banned by:",
                    message.author!!.mention,
                    false
                )
                field(
                    "Ban Reason:",
                    fixedReason,
                    false
                )
                color = Colors.error
            }
        }
        try {
            user.ban(
                server,
                deleteMessageDays,
                fixedReason
            )
            message.channel.send {
                embed {
                    field(
                        "${user.name}#${user.discriminator} was banned by:",
                        message.author!!.mention,
                        false
                    )
                    field(
                        "Ban Reason:",
                        fixedReason,
                        false
                    )
                    color = Colors.error
                }
            }
        } catch (e: Exception) {
            message.channel.send {
                embed {
                    title = "That user's role is higher then mine, I can't ban them!"
                    field("Stacktrace:", "```$e```")
                    color = Colors.error
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "$fullName <user(id, username, ping)>\n" +
                "$fullName <user(id, username, ping)> <reason>\n" +
                "$fullName <user(id, username, ping)> <delete messages (boolean)> <reason>"
    }
}