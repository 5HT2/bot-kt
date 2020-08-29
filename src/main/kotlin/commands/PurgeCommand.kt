package commands

import Command
import arg
import doesLater
import integer
import long
import net.ayataka.kordis.entity.deleteAll

object PurgeCommand : Command("purge") {
    init {
        integer("number") {
            doesLater { context ->
                if (!server!!.members.find(message.author!!.id)!!.canManage(message.serverChannel!!)) {
                    //Main.missingPermissionEmbed(message.channel) assuming my other pr gets merged (#20) this can be used...
                    return@doesLater
                }
                val number: Int = context arg "number"
                message.channel.getMessages(number).deleteAll()
            }
            long("user") {
                doesLater { context ->
                    if (!server!!.members.find(message.author!!.id)!!.canManage(message.serverChannel!!)) {
                        //Main.missingPermissionEmbed(message.channel) assuming my other pr gets merged (#20) this can be used...
                        return@doesLater
                    }
                    val number: Int = context arg "number"
                    val user: Long = context arg "user"
                    val search = if (number < 1000) number * 2 + 50 else number
                    message.channel.getMessages(search).filter { it.author!!.id == user }.take(number).deleteAll()
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Purges a number of messages in a channel based on parameters.\n" +
                "`;$name <number>`" +
                "`;$name <number> <userid>`"
    }
}
