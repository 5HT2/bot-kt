package commands

import Colors
import Command
import ConfigManager.readConfigSafe
import ConfigType
import Main
import PermissionTypes
import Permissions.hasPermission
import Send.error
import UserConfig
import arg
import bool
import doesLater
import greedyString
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.exception.MissingPermissionsException
import string

/**
 * @throws IllegalArgumentException
 */
object BanCommand : Command("ban") {
    init {
        string("user") {
            bool("deleteMessageDays") {
                greedyString("reason") {
                    doesLater { context ->
                        if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                            return@doesLater
                        }
                        val username: String = context arg "user"
                        val serverId = readConfigSafe<UserConfig>(ConfigType.USER, false)?.primaryServerId
                        val user: User =
                            Main.client?.servers?.find(serverId ?: message.server!!.id)?.members?.findByName(username)
                                ?: //username
                                Main.client?.servers?.find(
                                    serverId ?: message.server!!.id
                                )?.members?.find { it.nickname == username } ?: //nick
                                Main.client?.servers?.find(
                                    serverId ?: message.server!!.id
                                )?.members?.find(username.toLong()) ?: //id
                                run {
                                    message.error("User $username not found!")
                                    return@doesLater
                                }
                        val messageDays: Boolean = context arg "deleteMessageDays"
                        val fixedDays = if (messageDays) 1 else 0
                        val reason: String = context arg "reason"
                        val fixedReason =
                            if (reason.isEmpty()) readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultBanReason
                                ?: "No Reason Specified" else reason
                        if (user.id == message.author?.id) {
                            message.error("You can't ban yourself!")
                            return@doesLater
                        } else if (hasPermission(user.id, PermissionTypes.COUNCIL_MEMBER)) {
                            message.error("That user is protected, I can't do that.")
                            return@doesLater
                        }
                        try {
                            user.ban(
                                Main.client?.servers?.find(serverId ?: message.server!!.id) ?: run {
                                    message.error("Bad server defined in config!")
                                    return@doesLater
                                },
                                fixedDays,
                                fixedReason
                            )
                        } catch (e: Exception) {
                            message.channel.send {
                                embed {
                                    title = "Failed to ban the user!"
                                    field("Stacktrace:", "```$e```")
                                    color = Colors.error
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "$fullName <user(id, username, nick)> <delete message days (boolean)> <reason>"
    }
}