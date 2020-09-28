package commands

import Command
import PermissionTypes
import Permissions.hasPermission
import doesLater

object UnSlowCommand : Command("unslow") {
    init {
        doesLater {
            if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                return@doesLater
            }
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
