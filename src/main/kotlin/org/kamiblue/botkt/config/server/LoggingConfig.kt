package org.kamiblue.botkt.config.server

import org.kamiblue.botkt.config.ServerConfig

class LoggingConfig : ServerConfig("Logging") {
    val ignoredChannels by setting("Ignored Channels", HashSet<Long>(), "ID of channels to not log")
    val ignoredPrefix by setting("Ignored Prefix", ";", "Ignore message with specific prefix when edited by council member")
    val loggingChannel by setting("Logging Channel", -1L, "ID of the channel to send the logs")
}
