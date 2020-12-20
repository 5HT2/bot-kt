package org.kamiblue.botkt.manager.managers

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.event.events.ShutdownEvent
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.capeapi.AbstractUUIDManager
import org.kamiblue.event.listener.listener

object UUIDManager : AbstractUUIDManager("cache/uuid.json"), Manager {
    init {
        try {
            load()
        } catch (e :Exception) {
            Main.logger.warn("Failed to load UUID cache", e)
        }

        listener<ShutdownEvent> {
            try {
                load()
            } catch (e :Exception) {
                Main.logger.warn("Failed to save UUID cache", e)
            }
        }
    }

    override fun logError(message: String) {
        throw UUIDFormatException(message)
    }

    class UUIDFormatException(s: String) : IllegalArgumentException(s)
}