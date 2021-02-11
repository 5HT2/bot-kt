package org.kamiblue.botkt.config.server

import org.kamiblue.botkt.config.ServerConfig

class JoinLeaveConfig : ServerConfig("Join Leave") {
    val joinChannel by setting("Join Channel", -1L)
    val leaveChannel by setting("Leave Channel", -1L)
    val banChannel by setting("Ban Channel", -1L)
    val embed by setting("Embed", true, "Send the join leave logging message as embed.")
    val kickTooNew by setting("Kick Too New", false, "Kicks account less than 24 hours old.")
    val banRepeatedJoin by setting("Ban Repeated Join", false, "Ban accounts after 3 kicks.")
}
