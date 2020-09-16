package commands

import Command
import PermissionTypes
import Permissions.hasPermission
import arg
import doesLater
import integer

object SlowCommand : Command("slow") {
    init {
        integer("wait") {
            doesLater { context ->
                if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
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
