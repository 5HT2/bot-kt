package org.kamiblue.botkt.plugin

import org.kamiblue.botkt.Main
import org.kamiblue.commons.collections.NameableSet
import java.io.File
import java.io.FileNotFoundException

object PluginManager {

    internal val loadedPlugins = NameableSet<Plugin>()

    private val lockObject = Any()
    private val pluginPath = "plugins/"

    internal fun preLoad(): List<Plugin> {
        // Create directory if not exist
        val dir = File(pluginPath)
        if (!dir.exists()) dir.mkdir()

        val files = dir.listFiles() ?: return emptyList()
        val jarFiles = files.filter { it.extension.equals("jar", true) }
        val plugins = ArrayList<Plugin>()

        jarFiles.forEach {
            try {
                val loader = PluginLoader(it)
                plugins.add(loader.load())
            } catch (e: FileNotFoundException) {
                Main.logger.info("${it.name} is not a valid plugin, skipping")
            } catch (e: Exception) {
                Main.logger.error("Failed to load plugin ${it.name}", e)
            }
        }

        return plugins
    }

    internal fun loadAll(plugins: List<Plugin>) {
        synchronized(lockObject) {
            plugins.forEach {
                it.onLoad()
                it.register()
                loadedPlugins.add(it)
            }
        }
        Main.logger.info("Loaded ${loadedPlugins.size} plugins!")
    }

    internal fun load(plugin: Plugin) {
        synchronized(lockObject) {
            plugin.onLoad()
            plugin.register()
            loadedPlugins.add(plugin)
        }
        Main.logger.info("Loaded plugin $plugin")
    }

    internal fun unloadAll() {
        synchronized(lockObject) {
            loadedPlugins.forEach {
                it.unregister()
                it.onUnload()
            }
            loadedPlugins.clear()
        }
        Main.logger.info("Unloaded all plugins!")
    }

    internal fun unload(plugin: Plugin) {
        synchronized(lockObject) {
            if (loadedPlugins.remove(plugin)) {
                plugin.unregister()
                plugin.onUnload()
            }
        }
    }

}