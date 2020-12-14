package org.kamiblue.botkt.manager

import org.kamiblue.botkt.event.BotEventBus
import org.kamiblue.botkt.utils.MessageSendUtils
import org.kamiblue.commons.utils.ClassUtils

object ManagerLoader {

    fun load() {
        val managerClasses = ClassUtils.findClasses("org.kamiblue.botkt.manager.managers", Manager::class.java)

        for (clazz in managerClasses) {
            ClassUtils.getInstance(clazz).also { BotEventBus.subscribe(it) }
        }

        MessageSendUtils.log("Loaded ${managerClasses.size} managers!")
    }

}