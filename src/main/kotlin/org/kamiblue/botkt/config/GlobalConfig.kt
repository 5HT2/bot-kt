package org.kamiblue.botkt.config

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.event.BotEventBus
import org.kamiblue.botkt.event.events.ShutdownEvent
import org.kamiblue.event.listener.listener

open class GlobalConfig(name: String) : AbstractConfig(name) {
    final override val path: String
        get() = "configs"

    companion object {
        private val globalConfigs = LinkedHashSet<GlobalConfig>()

        init {
            listener<ShutdownEvent> {
                saveAll()
            }

            BotEventBus.subscribe(this)
        }

        fun register(config: GlobalConfig) {
            config.load()
            globalConfigs.add(config)
        }

        fun unregister(config: GlobalConfig) {
            globalConfigs.remove(config)
        }

        fun loadAll(): Boolean {
            return globalConfigs.runCatchingAll(GlobalConfig::load) { it, e ->
                Main.logger.warn("Failed to load global config ${it.name}", e)
            }
        }

        fun saveAll(): Boolean {
            return globalConfigs.runCatchingAll(GlobalConfig::save) { it, e ->
                Main.logger.warn("Failed to save global config ${it.name}", e)
            }
        }

        fun load(config: GlobalConfig): Boolean {
            return try {
                checkRegistered(config)
                config.load()
                true
            } catch (e: Exception) {
                Main.logger.warn("Failed to load global config ${config.name}", e)
                false
            }
        }

        fun save(config: GlobalConfig): Boolean {
            return try {
                checkRegistered(config)
                config.save()
                true
            } catch (e: Exception) {
                Main.logger.warn("Failed to save global config ${config.name}", e)
                false
            }
        }

        private fun checkRegistered(config: GlobalConfig) {
            if (!globalConfigs.contains(config)) {
                throw IllegalArgumentException("Global config type ${config.name} is not registered")
            }
        }
    }
}
