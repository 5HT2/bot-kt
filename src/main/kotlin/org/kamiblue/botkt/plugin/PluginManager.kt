package org.kamiblue.botkt.plugin

import org.kamiblue.botkt.Main
import org.kamiblue.commons.collections.NameableSet
import java.io.File

internal object PluginManager {

    val loadedPlugins = NameableSet<Plugin>()
    val pluginLoaderMap = HashMap<Plugin, PluginLoader>()
    const val pluginPath = "plugins/"

    private val lockObject = Any()

    fun preLoad(): List<PluginLoader> {
        // Create directory if not exist
        val dir = File(pluginPath)
        if (!dir.exists()) dir.mkdir()

        val files = dir.listFiles() ?: return emptyList()
        val jarFiles = files.filter { it.extension.equals("jar", true) }
        val plugins = ArrayList<PluginLoader>()

        jarFiles.forEach {
            try {
                val loader = PluginLoader(it)
                loader.verify()
                plugins.add(loader)
            } catch (e: ClassNotFoundException) {
                Main.logger.info("${it.name} is not a valid plugin, skipping.")
            } catch (e: Exception) {
                Main.logger.error("Failed to prepare plugin ${it.name}!", e)
            }
        }

        return plugins
    }

    fun loadAll(plugins: List<PluginLoader>) {
        synchronized(lockObject) {
            plugins.forEach {
                load(it)
            }
        }
        Main.logger.info("Loaded ${loadedPlugins.size} plugins!")
    }

    fun load(loader: PluginLoader) {
        val plugin = synchronized(lockObject) {
            val plugin = loader.load()
            plugin.onLoad()
            plugin.register()
            loadedPlugins.add(plugin)
            pluginLoaderMap[plugin] = loader
            plugin
        }
        Main.logger.info("Loaded plugin ${plugin.name}!")
    }

    fun unloadAll() {
        synchronized(lockObject) {
            loadedPlugins.forEach {
                it.unregister()
                it.onUnload()
                pluginLoaderMap[it]?.close()
            }
            loadedPlugins.clear()
        }
        Main.logger.info("Unloaded all plugins!")
    }

    fun unload(plugin: Plugin) {
        synchronized(lockObject) {
            if (loadedPlugins.remove(plugin)) {
                plugin.unregister()
                plugin.onUnload()
                pluginLoaderMap[plugin]?.close()
            }
        }
        Main.logger.info("Unloaded plugin ${plugin.name}!")
    }

}