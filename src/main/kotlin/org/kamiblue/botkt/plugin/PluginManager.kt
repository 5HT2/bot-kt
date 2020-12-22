package org.kamiblue.botkt.plugin

import org.kamiblue.botkt.Main
import org.kamiblue.commons.collections.NameableSet
import java.io.File
import java.io.FileNotFoundException

object PluginManager {

    internal val loadedPlugins = NameableSet<Plugin>()
    private val pluginLoaderMap = HashMap<Plugin, PluginLoader>()

    private val lockObject = Any()
    private val pluginPath = "plugins/"

    internal fun preLoad(): List<PluginLoader> {
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
            } catch (e: FileNotFoundException) {
                Main.logger.info("${it.name} is not a valid plugin, skipping")
            } catch (e: Exception) {
                Main.logger.error("Failed to prepare plugin ${it.name}", e)
            }
        }

        return plugins
    }

    internal fun loadAll(plugins: List<PluginLoader>) {
        synchronized(lockObject) {
            plugins.forEach {
                val plugin = it.load()
                plugin.onLoad()
                plugin.register()
                loadedPlugins.add(plugin)
                pluginLoaderMap[plugin] = it
            }
        }
        Main.logger.info("Loaded ${loadedPlugins.size} plugins!")
    }

    internal fun load(loader: PluginLoader) {
        val plugin = synchronized(lockObject) {
            val plugin = loader.load()
            plugin.onLoad()
            plugin.register()
            loadedPlugins.add(plugin)
            pluginLoaderMap[plugin] = loader
            plugin
        }
        Main.logger.info("Loaded plugin ${plugin.name}")
    }

    internal fun unloadAll() {
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

    internal fun unload(plugin: Plugin) {
        synchronized(lockObject) {
            if (loadedPlugins.remove(plugin)) {
                plugin.unregister()
                plugin.onUnload()
                pluginLoaderMap[plugin]?.close()
            }
        }
    }

}