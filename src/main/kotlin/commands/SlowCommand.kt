package commands

import Command
import PermissionTypes.COUNCIL_MEMBER
import arg
import doesLaterIfHas
import integer

object SlowCommand : Command("slow") {
    init {
        integer("wait") {
            doesLaterIfHas(COUNCIL_MEMBER) { context ->
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
                "`$fullName 10`"
    }
}
