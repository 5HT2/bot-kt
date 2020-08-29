import FileManager.authConfigData
import FileManager.mutesConfigData
import FileManager.userConfigData
import FileManager.versionConfigData
import com.google.gson.Gson
import java.net.URL
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
    var versionConfigData: VersionConfig? = null
    var userConfigData: UserConfig? = null

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
        return if (configType.data != null && !reload) {
            configType.data as T?
        } else if (StringHelper.isUrl(configType.configPath)) {
            readConfigFromUrl<T>(configType)
        } else {
            readConfigFromFile<T>(configType)
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
                gson.fromJson(it, T::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads config file from a remote URL. Use readConfig() instead, with reload set to true if you need to refresh from the URL.
     *
     * [configType] is the type of config you'd like to return
     * [T] is [configType].clazz
     */
    inline fun <reified T> readConfigFromUrl(configType: ConfigType): T? {
        return try {
            gson.fromJson(URL(configType.configPath).readText(Charsets.UTF_8), T::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * [configPath] is the file name on disk, OR a remote URL. If it is a URL, it must be a valid URL which includes http/https as a prefix
 * [data] is the actual config data, read in the format of clazz
 * [clazz] is the associated class with the [data] format
 */
enum class ConfigType(val configPath: String, var data: Any?, val clazz: Class<*>) {
    AUTH("config/auth.json", authConfigData, AuthConfig::class.java),
    MUTE("config/mutes.json", mutesConfigData, MuteConfig::class.java),
    VERSION("https://raw.githubusercontent.com/kami-blue/bot-kt/master/version.json", versionConfigData, VersionConfig::class.java),
    USER("config/user.json", userConfigData, UserConfig::class.java)
}

/**
 * [botToken] is the token given to you from https://discord.com/developers/applications/BOT_ID_HERE/bot
 * [githubToken] can be generated with the full "repo" access checked https://github.com/settings/tokens
 */
data class AuthConfig(val botToken: String, val githubToken: String)

/**
 * [id] is the user snowflake ID
 * [unixUnmute] is the UNIX time of when they should be unmuted.
 * When adding a new [unixUnmute] time, it should be current UNIX time + mute time in seconds
 */
data class MuteConfig(val id: Long, val unixUnmute: Long)

/**
 * [version] is a semver format version String
 * Checked by comparing [Main.currentVersion] against https://raw.githubusercontent.com/kami-blue/bot-kt/master/version.json
 */
data class VersionConfig(val version: String)

/**
 * [autoUpdate] is whether the bot should automatically update after a successful update check. Will not do anything when set to true if update checking is disabled.
 * [primaryServerId] is the main server where startup messages should be sent. Omit from config to send to all servers.
 * [startUpChannel] is the channel name of where to send bot startup messages. Omit from config to disable startup messages.
 */
data class UserConfig(val autoUpdate: Boolean, val primaryServerId: Long?, val startUpChannel: String?)
