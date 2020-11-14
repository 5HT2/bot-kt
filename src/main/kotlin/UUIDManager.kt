package org.kamiblue.botkt

import org.kamiblue.capeapi.AbstractUUIDManager
import java.io.File

object UUIDManager : AbstractUUIDManager("cache/uuids.json") {
    override fun log(message: String) { }
}