package commands

import Command
import arg
import doesLater
import string

object RulesCommand : Command("r") {
    init {
        string("rule") {
            doesLater { context ->
                val ruleName: String = context arg "rule"
                message.channel.send(StringHelper.getRule(message.server!!, ruleName)?: "$ruleName does not exist.")
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Rule command, gets rule from rules channel:\n" +
                "`;$name [rule]`"
    }
}
