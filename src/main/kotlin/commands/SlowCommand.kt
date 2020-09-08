package commands

import Command
import PermissionTypes
import Permissions
import StringHelper
import StringHelper.MessageTypes.MISSING_PERMISSIONS
import arg
import doesLater
import integer

object SlowCommand : Command("slow") {
    init {
        integer("wait") {
            doesLater { context ->
                if (!Permissions.hasPermission(message, PermissionTypes.COUNCIL_MEMBER)) {
                    StringHelper.sendMessage(message.channel, MISSING_PERMISSIONS)
                    return@doesLater
                }
                val wait: Int = context arg "wait"
                message.serverChannel!!.edit {
                    rateLimitPerUser = wait
                }
            }

        }
    }

    override fun getHelpUsage(): String {
        return "Enables slowmode for this channel, specifying the wait in seconds.\n\n" +
                "Usage:\n" +
                "`;$name 10`"
    }
}
