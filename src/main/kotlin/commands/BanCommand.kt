package commands

import Command
import ConfigManager.readConfigSafe
import ConfigType
import Main
import Permissions.hasPermission
import UserConfig
import arg
import doesLater
import greedyString
import integer
import long

object BanCommand : Command("ban") {
    init {
        long("user") {
            integer("deleteMessageDays") {
                greedyString("reason") {
                    doesLater { context ->
                        if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                            return@doesLater
                        }
                        val id: Long = context arg "user"
                        val messageDays: Int = context arg "deleteMessageDays"
                        val fixedDays = if (messageDays >= 7) 7 else messageDays
                        val reason: String = context arg "reason"
                        val user = Main.client!!.servers.find(
                            readConfigSafe<UserConfig>(
                                ConfigType.USER,
                                false
                            )?.primaryServerId ?: 735248256073990165
                        )?.members?.find(id) ?: run {
                            message.channel.send("Member not found!")
                            return@doesLater
                        }
                        user.ban(
                            Main.client?.servers?.find(
                                readConfigSafe<UserConfig>(
                                    ConfigType.USER,
                                    false
                                )?.primaryServerId ?: 735248256073990165
                            ) ?: throw IllegalArgumentException("This is impossible to be thrown"), fixedDays, reason
                        )
                    }

                }
            }
//        string("user") {
//            integer("deleteMessageDays") {
//                greedyString("reason") {
//                    doesLater { context ->
////                        if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
////                            return@doesLater
////                        }
//                        val id: String = context arg "user"
//                        val user: User?
//                        if (message.content.contains('@')){
//                            user = Main.client?.users?.find(id.filter { it.isDigit() }.toLong()) ?: run {
//                                message.channel.send("User not found! Aborting.")
//                                return@doesLater
//                            }
//                        } else {
//                            TODO("Username search not implemented")
//                        }
//                        val messageDays: Int = context arg "deleteMessageDays"
//                        val reason: String = context arg "reason"
//                        val server = readConfigSafe<UserConfig>(ConfigType.USER, false)?.primaryServerId?.let { Main.client?.servers?.find(it) } ?: run { return@doesLater }
//                        //user?.ban(server, messageDays, reason)
//                        if (message.author == user){
//                            message.channel.send("Do you want to get yourself banned?")
//                        }
//                        message.channel.send("${user.name} is banned in ${server.name} because of $reason and message deleted by $messageDays days.")
//                    }
//                }
//            }
//        }
        }
    }
}