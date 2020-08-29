package commands

import Command
import arg
import doesLater
import greedyString
import integer
import literal
import string

object PurgeCommand : Command("purge") {
    init {
        integer("number") {
            doesLater { context ->
                if (!server!!.members.find(message.author!!.id)!!.canManage(message.serverChannel!!)) {
                    //Main.missingPermissionEmbed(message.channel) assuming my other pr gets merged (#20) this can be used...
                    return@doesLater
                }
                val number: Int = context arg "number"
                for (message in message.channel.getMessages(number)) {
                    message.delete()
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Purges a number of messages in a channel based on parameters.\n" +
                "`;$name <number>`"
    }
}
