package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.ConfigManager
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.RulesConfig
import org.kamiblue.botkt.command.CommandOld
import org.kamiblue.botkt.command.arg
import org.kamiblue.botkt.command.doesLater
import org.kamiblue.botkt.command.string
import org.kamiblue.botkt.utils.Colors

object RulesCommand : CommandOld("r") {
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
                            color = Colors.ERROR.color
                        }
                    } else {
                        embed {
                            title = "Rule $ruleName"
                            description = rule
                            color = Colors.PRIMARY.color
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
