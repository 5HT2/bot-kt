package org.kamiblue.botkt.commands

import net.ayataka.kordis.entity.deleteAll
import org.kamiblue.botkt.*

object PurgeCommand : Command("purge") {
    init {
        integer("number") {
            doesLaterIfHas(PermissionTypes.COUNCIL_MEMBER) { context ->
                val number: Int = context arg "number"
                message.channel.getMessages(number + 1).deleteAll()
            }
            greedyString("user") {
                doesLaterIfHas(PermissionTypes.COUNCIL_MEMBER) { context ->
                    val contextNumber: Int = context arg "number"
                    val number = contextNumber + 1 // include original message to delete
                    val user: String = context arg "user"
                    val search = if (number < 1000) number * 2 + 50 else number
                    message.channel.getMessages(search).filter {
                        it.author?.id.toString() == user || it.author?.mention == user || it.author?.tag == user
                    }.take(number).deleteAll()
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
