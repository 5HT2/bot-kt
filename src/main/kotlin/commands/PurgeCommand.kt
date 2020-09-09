package commands

import Command
import StringHelper.MessageTypes.MISSING_PERMISSIONS
import arg
import doesLater
import greedyString
import integer
import net.ayataka.kordis.entity.deleteAll

object PurgeCommand : Command("purge") {
    init {
        integer("number") {
            doesLater { context ->
                if (!Permissions.hasPermission(message, PermissionTypes.COUNCIL_MEMBER)) {
                    StringHelper.sendMessage(message.channel, MISSING_PERMISSIONS)
                    return@doesLater
                }
                val number: Int = context arg "number"
                message.channel.getMessages(number + 1).deleteAll()
            }
            greedyString("user") {
                doesLater { context ->
                    if (!Permissions.hasPermission(message, PermissionTypes.COUNCIL_MEMBER)) {
                        StringHelper.sendMessage(message.channel, MISSING_PERMISSIONS)
                        return@doesLater
                    }
                    val contextNumber: Int = context arg "number"
                    val number = contextNumber + 1 // include original message to delete
                    val user: String = context arg "user"
                    val search = if (number < 1000) number * 2 + 50 else number
                    message.channel.getMessages(search).filter { it.author!!.id.toString() == user || it.author!!.mention == user || it.author!!.tag == user }.take(number).deleteAll()
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Purges a number of messages in a channel based on parameters.\n" +
                "`;$name <number>`\n" +
                "`;$name <number> <userid>`"
    }
}
