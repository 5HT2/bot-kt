package org.kamiblue.botkt.manager.managers

import org.kamiblue.botkt.manager.Manager
import org.kamiblue.capeapi.AbstractUUIDManager

object UUIDManager : AbstractUUIDManager("cache/uuids.json"), Manager {
    override fun logError(message: String) {
        throw UUIDFormatException(message)
    }

    class UUIDFormatException(s: String) : IllegalArgumentException(s)
}