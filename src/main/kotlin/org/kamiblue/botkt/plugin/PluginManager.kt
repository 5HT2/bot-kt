package org.kamiblue.botkt.plugin

import org.kamiblue.botkt.Main
import org.kamiblue.commons.collections.NameableSet
import java.io.File
import java.io.FileNotFoundException

internal object PluginManager {
    val loadedPlugins = NameableSet<Plugin>()
    val pluginLoaderMap = HashMap<Plugin, PluginLoader>()

    const val pluginPath = "plugins/"

    private val lockObject = Any()
    private val botVersion = convertVersionDirty(Main.currentVersion)

    fun getLoaders(): List<PluginLoader> {
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
                Main.logger.info("${it.name} is not a valid plugin. Skipping...")
            } catch (e: PluginInfoMissingException) {
                Main.logger.warn("${it.name} is missing a required info ${e.infoName}. Skipping...", e)
            } catch (e: Exception) {
                Main.logger.error("Failed to pre load plugin ${it.name}", e)
            }
        }

        return plugins
    }

    fun loadAll(loaders: List<PluginLoader>) {
        val validLoaders = checkPluginLoaders(loaders)

        synchronized(lockObject) {
            validLoaders.forEach(::loadWithoutCheck)
        }

        Main.logger.info("Loaded ${loadedPlugins.size} plugins!")
    }

    private fun checkPluginLoaders(loaders: List<PluginLoader>): List<PluginLoader> {
        val loaderSet = NameableSet<PluginLoader>()
        val invalids = HashSet<PluginLoader>()

        for (loader in loaders) {
            // Hot reload check, the error shouldn't be show when reload in game
            if (Main.ready && !loader.info.hotReload) {
                invalids.add(loader)
            }

            // Unsupported check
            if (convertVersionDirty(loader.info.botVersion) > botVersion) {
                PluginError.UNSUPPORTED.handleError(loader)
                invalids.add(loader)
            }

            // Duplicate check
            loaderSet[loader.name]?.let {
                PluginError.DUPLICATE.handleError(loader)
                invalids.add(loader)
                PluginError.DUPLICATE.handleError(it)
                invalids.add(it)
            } ?: run {
                loaderSet.add(loader)
            }
        }

        for (loader in loaders) {
            // Required plugin check
            if (!loadedPlugins.containsNames(loader.info.requiredPlugins)
                && !loaderSet.containsNames(loader.info.requiredPlugins)
            ) {
                PluginError.REQUIRED_PLUGIN.handleError(loader)
                invalids.add(loader)
            }
        }

        return loaders.filter { !invalids.contains(it) }
    }

    fun load(loader: PluginLoader) {
        synchronized(lockObject) {
            val hotReload = Main.ready && !loader.info.hotReload
            val duplicate = loadedPlugins.containsName(loader.name)
            val unsupported = convertVersionDirty(loader.info.version) > botVersion
            val missing = !loadedPlugins.containsNames(loader.info.requiredPlugins)

            if (hotReload) PluginError.HOT_RELOAD.handleError(loader)
            if (duplicate) PluginError.DUPLICATE.handleError(loader)
            if (unsupported) PluginError.UNSUPPORTED.handleError(loader)
            if (missing) PluginError.REQUIRED_PLUGIN.handleError(loader)

            if (hotReload || duplicate || unsupported || missing) return

            loadWithoutCheck(loader)
        }
    }

    private fun loadWithoutCheck(loader: PluginLoader) {
        val plugin = synchronized(lockObject) {
            val plugin = runCatching(loader::load).getOrElse {
                when (it) {
                    is ClassNotFoundException -> {
                        Main.logger.warn("Main class not found in plugin $loader", it)
                    }
                    is IllegalAccessException -> {
                        Main.logger.warn(it.message, it)
                    }
                    else -> {
                        Main.logger.error("Failed to load plugin $loader", it)
                    }
                }
                return
            }

            plugin.onLoad()
            plugin.register()
            loadedPlugins.add(plugin)
            pluginLoaderMap[plugin] = loader
            plugin
        }

        Main.logger.info("Loaded plugin ${plugin.name}")
    }

    fun unloadAll() {
        loadedPlugins.filter { it.hotReload }.forEach(::unloadWithoutCheck)

        Main.logger.info("Unloaded all plugins!")
    }

    fun unload(plugin: Plugin) {
        if (loadedPlugins.any { it.requiredPlugins.contains(plugin.name) }) {
            throw IllegalArgumentException("Plugin $plugin is required by another plugin!")
        }

        unloadWithoutCheck(plugin)
    }

    private fun unloadWithoutCheck(plugin: Plugin) {
        if (!plugin.hotReload) {
            throw IllegalArgumentException("Plugin $plugin cannot be hot reloaded!")
        }

        synchronized(lockObject) {
            if (loadedPlugins.remove(plugin)) {
                plugin.unregister()
                plugin.onUnload()
                pluginLoaderMap[plugin]?.close()
            }
        }

        Main.logger.info("Unloaded plugin ${plugin.name}")
    }

    private fun convertVersionDirty(version: String) = version
        .split('.')
        .map { string -> string.filter { it.isDigit() }.toInt() }.let {
            it[0] * 100 + it[1] * 10 + it[2]
        }

}
