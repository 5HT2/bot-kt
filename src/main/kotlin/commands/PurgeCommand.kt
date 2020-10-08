package commands

import Command
import PermissionTypes.COUNCIL_MEMBER
import arg
import doesLaterIfHas
import greedyString
import integer
import net.ayataka.kordis.entity.deleteAll

object PurgeCommand : Command("purge") {
    init {
        integer("number") {
            doesLaterIfHas(COUNCIL_MEMBER) { context ->
                val number: Int = context arg "number"
                message.channel.getMessages(number + 1).deleteAll()
            }
            greedyString("user") {
                doesLaterIfHas(COUNCIL_MEMBER) { context ->
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
                "`$fullName <number>`\n" +
                "`$fullName <number> <userid>`"
    }
}
