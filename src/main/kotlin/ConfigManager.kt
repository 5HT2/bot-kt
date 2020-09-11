import StringHelper.isUrl
import com.google.gson.Gson
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author dominikaaaa
 * @since 2020/08/16 19:48
 */
object ConfigManager {

    fun writeConfig(configType: ConfigType) {

    }

    /**
     * Safely returns [readConfig] without worrying if the file exists.
     *
     * [reload] will reload the file in memory and return the new file dataMap
     * [configType] is the type of config you'd like to return
     * [T] is [configType].clazz
     */
    inline fun <reified T> readConfigSafe(configType: ConfigType, reload: Boolean): T? {
        return if (configType.configPath.isUrl()) {
            readConfig<T>(configType, reload)
        } else {
            if (File(configType.configPath).exists()) {
                readConfig<T>(configType, reload)
            } else {
                null
            }
        }
    }

    /**
     * Reads config from memory if it's already been read.
     *
     * [reload] will reload the file in memory and return the new file dataMap
     * [configType] is the type of config you'd like to return
     * [T] is [configType].clazz
     *
     * @throws [NoSuchFileException]
     */
    inline fun <reified T> readConfig(configType: ConfigType, reload: Boolean): T? {
        return if (configType.data != null && !reload) {
            configType.data as T?
        } else if (configType.configPath.isUrl()) {
            readConfigFromUrl<T>(configType)
        } else {
            val config = readConfigFromFile<T>(configType)
            config?.let { configType.data = it }
            config
        }
    }

    /**
     * Reads config file from disk. Use readConfig() instead, with reload set to true if you need to refresh from disk.
     *
     * [configType] is the type of config you'd like to return
     * [T] is [configType].clazz
     */
    inline fun <reified T> readConfigFromFile(configType: ConfigType): T? {
        return try {
            Files.newBufferedReader(Paths.get(configType.configPath)).use {
                Gson().fromJson(it, T::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads config file from a remote URL. Preferably use readConfig() instead.
     *
     * [configType] is the type of config you'd like to return
     * [T] is [configType].clazz
     */
    inline fun <reified T> readConfigFromUrl(configType: ConfigType): T? {
        return try {
            Gson().fromJson(URL(configType.configPath).readText(Charsets.UTF_8), T::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads config file from a remote URL. Preferably use readConfig() instead.
     */
    inline fun <reified T> readConfigFromUrl(url: String): T? {
        return try {
            Gson().fromJson(URL(url).readText(Charsets.UTF_8), T::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
