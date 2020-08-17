import FileManager.authConfigData
import FileManager.mutesConfigData
import com.google.gson.Gson
import net.ayataka.kordis.entity.user.User
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Paths


/**
 * @author dominikaaaa
 * @since 2020/08/16 19:48
 */
object FileManager {
    private val gson = Gson()
    var authConfigData: Map<*, *>? = null
    var mutesConfigData: Map<*, *>? = null

    fun writeConfig() {

    }

    fun readConfig(configType: ConfigType): Map<*, *>? {
        try {
            val reader: Reader = Files.newBufferedReader(Paths.get(configType.fileName))
            configType.dataMap = gson.fromJson(reader, configType.clazz) as Map<*,*>
            reader.close()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            return null
        }
        return configType.dataMap
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
enum class ConfigType(val fileName: String, var dataMap: Map<*, *>?, val clazz: Class<*>) {
    AUTH("auth.json", authConfigData, AuthConfig::class.java),
    MUTE("mutes.json", mutesConfigData, MuteConfig::class.java)
}

/**
 * [botToken] is the token given to you from https://discord.com/developers/applications/BOT_ID_HERE/bot
 * [githubToken] can be generated with the full "repo" access checked https://github.com/settings/tokens
 */
class AuthConfig {
    var botToken: String? = null
    var githubToken: String? = null


    fun AuthType(botToken: String?, githubToken: String?) {
        this.botToken = botToken
        this.githubToken = githubToken
    }
}

/**
 * [id] is the user snowflake ID
 * [unixUnmute] is the UNIX time of then they should be unmuted.
 * When adding a new [unixUnmute] time, it should be current UNIX time + mute time in seconds
 */
class MuteConfig {
    var id: Int? = null
    var unixUnmute: Int? = null

    fun MuteConfig(id: Int?, unixUnmute: Int?) {
        this.id = id
        this.unixUnmute = unixUnmute
    }
}