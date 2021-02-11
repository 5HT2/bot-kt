package org.kamiblue.botkt.command.commands.info

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.config.ServerConfigs
import org.kamiblue.botkt.config.server.RuleConfig
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
                val server = server?: return@execute
                val ruleName = ruleNameArg.value
                val ruleConfig = ServerConfigs.get<RuleConfig>(server)
                val rule = ruleConfig.rules[ruleName]

                message.channel.send {
                    if (rule == null) {
                        embed {
                            description = "Couldn't find rule $ruleName."
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
