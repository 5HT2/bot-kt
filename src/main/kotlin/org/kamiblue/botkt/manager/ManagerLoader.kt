package org.kamiblue.botkt.manager

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.event.BotEventBus
import org.kamiblue.commons.utils.ClassUtils
import org.kamiblue.commons.utils.ClassUtils.instance

internal object ManagerLoader {

    fun load() {
        val managerClasses = ClassUtils.findClasses<Manager>("org.kamiblue.botkt.manager.managers")

        for (clazz in managerClasses) {
            BotEventBus.subscribe(clazz.instance)
        }

        Main.logger.info("Loaded ${managerClasses.size} managers!")
    }
}
