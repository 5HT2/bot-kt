package commands

import Command
import PermissionTypes
import Permissions
import StringHelper
import StringHelper.MessageTypes.MISSING_PERMISSIONS
import doesLater

object UnSlowCommand : Command("unslow") {
    init {
        doesLater {
            if (!Permissions.hasPermission(message, PermissionTypes.COUNCIL_MEMBER)) {
                StringHelper.sendMessage(message.channel, MISSING_PERMISSIONS)
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
                "`;$name`"
    }
}
