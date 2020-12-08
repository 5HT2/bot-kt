package org.kamiblue.botkt.command.commands.info

import org.kamiblue.botkt.ConfigManager
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.RulesConfig
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.Colors

object RulesCommand : BotCommand(
    name = "rule",
    alias = arrayOf("r", "law"),
    category = Category.INFO,
    description = "Rule command, gets rule from rules channel"
) {
    init {
        string("ruleName") { ruleNameArg ->
            execute {
                val ruleName = ruleNameArg.value
                val rule = ConfigManager.readConfigSafe<RulesConfig>(ConfigType.RULES, false)?.rules?.getOrDefault(
                    ruleName,
                    "Couldn't find rule $ruleName."
                ) ?: "Couldn't find rule config file!"

                message.channel.send {
                    if (rule.startsWith("Couldn't find rule")) {
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
}
