package org.kamiblue.botkt.config.global

import org.kamiblue.botkt.config.GlobalConfig

object CapeConfig : GlobalConfig("Cape"){
    val capeCommit by setting("Cape Commit", true)
}
