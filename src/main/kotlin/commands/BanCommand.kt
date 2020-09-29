package commands

import Command
import ConfigManager.readConfigSafe
import ConfigType
import Main
import Permissions.hasPermission
import Send.error
import UserConfig
import arg
import doesLater
import greedyString
import integer
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.exception.MissingPermissionsException
import string

object BanCommand : Command("ban") {
    init {
        string("user") {
            integer("deleteMessageDays") {
                greedyString("reason") {
                    doesLater { context ->
                        if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                            return@doesLater
                        }
                        val username: String = context arg "user"
                        val serverId = readConfigSafe<UserConfig>(ConfigType.USER, false)?.primaryServerId
                        val user: User = Main.client?.servers?.find(serverId ?: 573954110454366214)?.members?.findByName(username) ?: //username
                        Main.client?.servers?.find(serverId ?: 573954110454366214)?.members?.find { it.nickname == username } ?: //nick
                        Main.client?.servers?.find(serverId ?: 573954110454366214)?.members?.find(username.toLong()) ?: //id
                        run {
                            message.error("User not found!")
                            return@doesLater
                        }
                        val messageDays: Int = context arg "deleteMessageDays"
                        val fixedDays = if (messageDays >= 7) 7 else messageDays
                        val reason: String = context arg "reason"
                        val fixedReason =
                            if (reason.isEmpty()) readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultReason
                                ?: "No Reason Specified" else reason
                        if (user.id == message.author?.id) {
                            message.error("You can't ban yourself!")
                            return@doesLater
                        }
                        try {
                            user.ban(
                                Main.client?.servers?.find(
                                    readConfigSafe<UserConfig>(
                                        ConfigType.USER,
                                        false
                                    )?.primaryServerId ?: 573954110454366214
                                ) ?: throw IllegalArgumentException("This is impossible to be thrown"),
                                fixedDays,
                                fixedReason
                            )
                        } catch(e: MissingPermissionsException){
                            message.channel.send {
                                embed {
                                    title = "That user is protected, I can't do that."
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
}