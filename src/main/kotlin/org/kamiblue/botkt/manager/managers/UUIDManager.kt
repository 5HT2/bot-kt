package org.kamiblue.botkt.manager.managers

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.event.events.ShutdownEvent
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.capeapi.AbstractUUIDManager
import org.kamiblue.event.listener.listener

object UUIDManager : AbstractUUIDManager(
    filePath = "cache/uuid.json",
    logger = Main.logger,
    maxCacheSize = 1000
), Manager {
    init {
        load()

        listener<ShutdownEvent> {
            save()
        }
    }
}