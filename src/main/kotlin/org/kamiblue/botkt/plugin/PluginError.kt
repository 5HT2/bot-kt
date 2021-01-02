package org.kamiblue.botkt.plugin

import org.kamiblue.botkt.Main

internal enum class PluginError {
    HOT_RELOAD,
    DUPLICATE,
    UNSUPPORTED,
    REQUIRED_PLUGIN;

    fun handleError(loader: PluginLoader) {
        val list = latestErrors ?: ArrayList<Pair<PluginLoader, PluginError>>().also { latestErrors = it }

        when (this) {
            HOT_RELOAD -> {
                Main.logger.error("Plugin $loader cannot be hot reloaded.")
            }
            DUPLICATE -> {
                Main.logger.error("Duplicate plugin $loader.")
            }
            UNSUPPORTED -> {
                Main.logger.error("Unsupported plugin $loader. Required version: ${loader.info.botVersion}")
            }
            REQUIRED_PLUGIN -> {
                Main.logger.error("Missing required plugin for $loader. Required plugins: ${loader.info.requiredPlugins.joinToString()}")
            }
        }

        list.add(loader to this)
    }

    companion object {
        private var latestErrors: ArrayList<Pair<PluginLoader, PluginError>>? = null

        fun getErrors(): List<Pair<PluginLoader, PluginError>>? {
            val errors = latestErrors
            latestErrors = null

            return if (!errors.isNullOrEmpty()) {
                errors
            } else {
                null
            }
        }
    }
}
