package commands

import Command
import arg
import doesLater
import greedyString

/**
 * @author dominikaaaa
 * @since 2020/08/18 16:30
 */
object RulesCommand : Command("r") {
    init {
        greedyString("rule") {
            doesLater { context ->
                // Explicit types are necessary for type inference
                val ruleName: String = context arg "rule"
                message.channel.send(RulesHelper.getRule(message.server!!, ruleName)?: "$ruleName does not exist.")
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Rule command, gets rule from rules channel:\n" +
                "`;$name [rule]`\n\n"
    }
}
