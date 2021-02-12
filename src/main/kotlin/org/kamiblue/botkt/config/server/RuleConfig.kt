package org.kamiblue.botkt.config.server

import org.kamiblue.botkt.config.ServerConfig

class RuleConfig : ServerConfig("Rule") {
    val rules by setting("Rules", HashMap<String, String>(), "Used by RulesCommand")
}
