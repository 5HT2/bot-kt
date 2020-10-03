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
import doesLater
import greedyString
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.user.User

object BanCommand : Command("ban") {
    init {
        greedyString("userAndReason") {
            doesLater { context -> // we unfortunately have to do really icky manual string parsing here, due to brigadier not knowing <@!id> is a string
                if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                    return@doesLater
                }
                val username: String = context arg "userAndReason"

                if (!username.contains(" ")) {
                    ban(username, false, null, server, message)
                    return@doesLater
                } else {
                    // split message in the format of [username, false/true, reason]
                    val splitWithDeleteMsgs = username.split(" ".toRegex(), 3)
                    var deleteMsgsReason: String? = null

                    try {
                        deleteMsgsReason = splitWithDeleteMsgs[2]
                    } catch (e: IndexOutOfBoundsException) {
                        // this is fine, it just means we just won't have a reason while deleting messages
                    }

                    if (splitWithDeleteMsgs[1] == "true") { // [username, *true*, reason]
                        ban(splitWithDeleteMsgs[0], true, deleteMsgsReason, server, message)
                        return@doesLater
                    } else if (splitWithDeleteMsgs[1] == "false") { // [username, *false*, reason]
                        ban(splitWithDeleteMsgs[0], false, deleteMsgsReason, server, message)
                        return@doesLater
                    }

                    // split message in the format of [username, reason], provided username does not contain spaces (it shouldn't!!)
                    val split = username.split(" ".toRegex(), 2)
                    if (split.size != 2) {
                        message.error("Failed to ban $username, this should not be possible. Size: `${split.size}`")
                        return@doesLater
                    }
                    ban(split[0], false, split[1], server, message)
                }
            }
        }
    }

    private suspend fun ban(
        unfilteredUsername: String, // this can be an @mention (<@id>), an ID (id), or a username (username#discrim)
        deleteMsgs: Boolean, // if we should delete the past day of their messages or not
        reason: String?, // reason why they were banned. dmed before banning
        nullableServer: Server?,
        message: Message
    ) {
        val server = nullableServer ?: run { message.error("Server is null, make sure you aren't running this from a DM!"); return }

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

        val fixedReason = if (reason != null && reason.isNotEmpty()) reason else readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultBanReason ?: "No Reason Specified"

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