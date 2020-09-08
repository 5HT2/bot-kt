package commands

import Command
import StringHelper.MessageTypes.MISSING_PERMISSIONS
import arg
import doesLater
import string

object ReloadRulesCommand : Command("reloadrules") {
    init {
        doesLater {
            if (!Permissions.hasPermission(message, PermissionTypes.COUNCIL_MEMBER)) {
                StringHelper.sendMessage(message.channel, MISSING_PERMISSIONS)
                return@doesLater
            }
            StringHelper.removeServerRules(message.server!!)
            message.channel.send("Rules have been reloaded...")
        }
    }

    override fun getHelpUsage(): String {
        return "Reloads the rules."
    }
}
