package commands

import Colors
import Command
import ConfigManager
import ConfigType
import RulesConfig
import arg
import doesLater
import string

object RulesCommand : Command("r") {
    init {
        string("rule") {
            doesLater { context ->
                val ruleName: String = context arg "rule"
                val rule = ConfigManager.readConfigSafe<RulesConfig>(ConfigType.RULES, false)?.rules?.getOrDefault(
                    ruleName,
                    "Couldn't find rule $ruleName."
                ) ?: "Couldn't find rule config file!"

                message.channel.send {
                    if (rule.contains("Couldn't find rule")) {
                        embed {
                            description = rule
                            color = Colors.error
                        }
                    } else {
                        embed {
                            title = "Rule $ruleName"
                            description = rule
                            color = Colors.primary
                        }
                    }
                }

            }
        }
    }

    override fun getHelpUsage(): String {
        return "Rule command, gets rule from rules channel:\n" +
                "`$fullName [rule]`"
    }
}
