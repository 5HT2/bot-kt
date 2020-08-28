package commands

import Command
import arg
import doesLater
import string

object ReloadRulesCommand : Command("reloadrules") {
    init {
        doesLater {
            StringHelper.removeServerRules(message.server!!)
            message.channel.send("Rules have been reloaded...")
        }
    }

    override fun getHelpUsage(): String {
        return "Reloads the rules."
    }
}
