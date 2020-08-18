import FileManager.authConfigData
import FileManager.mutesConfigData
import com.google.gson.Gson
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author dominikaaaa
 * @since 2020/08/16 19:48
 */
object FileManager {
    val gson = Gson()
    var authConfigData: AuthConfig? = null
    var mutesConfigData: MuteConfig? = null

    fun writeConfig(configType: ConfigType) {

    }

    /**
     * Reads config from memory if it's already been read.
     *
     * [reload] will reload the file in memory and return the new file dataMap
     * [configType] is the type of config you'd like to return
     * [T] is [configType].clazz
     */
    inline fun <reified T> readConfig(configType: ConfigType, reload: Boolean): T? {
        return if (configType.dataMap != null && !reload) {
            configType.dataMap as T?
        } else {
            readConfigFromFile<T>(configType)
        }
    }

    /**
     * Reads config file from disk. Use readConfig() instead, with reload set to true if you really need to refresh from disk.
     * 
     * [configType] is the type of config you'd like to return
     * [T] is [configType].clazz
     */
    inline fun <reified T> readConfigFromFile(configType: ConfigType): T? {
        return try {
            Files.newBufferedReader(Paths.get(configType.fileName)).use {
                gson.fromJson(it, T::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * [fileName] is the file name on disk
 * [dataMap] is a Map<*,*> in the format of <foo, bar> here:
 * {
 *     "foo": "bar"
 * }
 * [clazz] is the associated class with the [dataMap] format
 */
enum class ConfigType(val fileName: String, var dataMap: Any?, val clazz: Class<*>) {
    AUTH("auth.json", authConfigData, AuthConfig::class.java),
    MUTE("mutes.json", mutesConfigData, MuteConfig::class.java)
}

/**
 * [botToken] is the token given to you from https://discord.com/developers/applications/BOT_ID_HERE/bot
 * [githubToken] can be generated with the full "repo" access checked https://github.com/settings/tokens
 */
data class AuthConfig(val botToken: String, val githubToken: String)

/**
 * [id] is the user snowflake ID
 * [unixUnmute] is the UNIX time of then they should be unmuted.
 * When adding a new [unixUnmute] time, it should be current UNIX time + mute time in seconds
 */
data class MuteConfig(val id: Long, val unixUnmute: Long)
