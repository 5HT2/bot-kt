package commands

import Command
import doesLater

object UnSlowCommand : Command("unslow") {
    init {
        doesLater {
            if (!server!!.members.find(message.author!!.id)!!.canManage(message.serverChannel!!)) {
                message.channel.send {
                    embed {
                        field("Error", "You don't have permission to use this command!", true)
                        color = Main.Colors.ERROR.color
                    }
                }
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
