package commands

import Command
import doesLater

object UnSlowCommand : Command("unslow") {
    init {
        doesLater {
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
