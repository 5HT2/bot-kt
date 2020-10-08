package commands

import Command
import PermissionTypes.COUNCIL_MEMBER
import doesLaterIfHas

object UnSlowCommand : Command("unslow") {
    init {
        doesLaterIfHas(COUNCIL_MEMBER) {
            message.serverChannel!!.edit {
                rateLimitPerUser = 0
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Removes slowmode from this channel\n\n" +
                "Usage:\n" +
                "`$fullName`"
    }
}
