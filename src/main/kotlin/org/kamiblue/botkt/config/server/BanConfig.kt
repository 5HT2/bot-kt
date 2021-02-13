package org.kamiblue.botkt.config.server

import org.kamiblue.botkt.config.ServerConfig

class BanConfig : ServerConfig("Ban") {
    val defaultReason by setting("Default Reason", "No Reason Specified", "Default reason for ban")
}
