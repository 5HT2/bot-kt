package org.kamiblue.botkt.config.server

import org.kamiblue.botkt.config.ServerConfig

class StarboardConfig : ServerConfig("Starboard") {
    val channel by setting("Channel", -1L, "ID of the Starboard channel")
    val messages by setting("Messages", HashSet<Long>(), "Messages added to the starboard")
    val threshold by setting("Threshold", 3, "Amount of Star emoji reactions required to be added to starboard")
}
