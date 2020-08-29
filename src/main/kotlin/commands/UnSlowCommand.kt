package commands

import Command
import doesLater

object UnSlowCommand : Command("unslow") {
    init {
        doesLater {
            if (!server!!.members.find(message.author!!.id)!!.canManage(message.serverChannel!!)) {
                Main.missingPermissionEmbed(message.channel)
                return@doesLater
            }
            message.serverChannel!!.edit {
                rateLimitPerUser = 0
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Remove slow mode from this channel." +
                "`;$name`"
    }
}
