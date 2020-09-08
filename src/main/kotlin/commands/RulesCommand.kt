package commands

import Command
import RulesConfig
import arg
import doesLater
import string

object RulesCommand : Command("r") {
    init {
        string("rule") {
            doesLater { context ->
                val ruleName: String = context arg "rule"
                message.channel.send(FileManager.readConfigSafe<RulesConfig>(ConfigType.RULES, false)!!.rules.getOrDefault(ruleName, "No such rule $ruleName."))
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Rule command, gets rule from rules channel:\n" +
                "`;$name [rule]`"
    }
}
