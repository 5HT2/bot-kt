package org.kamiblue.botkt.config.server

import org.kamiblue.botkt.config.ServerConfig
import java.util.concurrent.ConcurrentHashMap

class MuteConfig : ServerConfig("Mute") {
    val muteMap by setting("Mute Map", ConcurrentHashMap<Long, Long>(), "Maps user id to their unmute UNIX time")
}
