package org.kamiblue.botkt

import org.kamiblue.capeapi.AbstractUUIDManager

object UUIDManager : AbstractUUIDManager("cache/uuids.json") {
    override fun logError(message: String) {
        throw UUIDFormatException(message)
    }

    class UUIDFormatException(s: String) : IllegalArgumentException(s)
}