import ConfigManager.readConfigSafe
import Send.error
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import helpers.StringHelper.toHumanReadable
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.permission.PermissionSet
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * @return a pretty formatted set of permissions, "None" if empty
 */
fun PermissionSet.pretty() =
    if (this.isEmpty()) "None"
    else this.joinToString { it.name.toHumanReadable() }

/**
 * @return [T] from [url]
 */
inline fun <reified T> request(url: String): T {
    val request = Request.Builder().url(url).get().build()
    val response = OkHttpClient().newCall(request).execute()

    return Gson().fromJson(response.body()!!.string(), T::class.java)
}

/**
 * [authType] is the type of header to be used. "Bot" is for Discord's API, while Github uses "token".
 * @return [T] from [url] with the [token] as the Authorization header
 */
inline fun <reified T> authenticatedRequest(authType: String, token: String, url: String): T {
    val request = Request.Builder().addHeader("Authorization", "$authType $token").url(url).get().build()
    val response = OkHttpClient().newCall(request).execute()

    return Gson().fromJson(response.body()!!.string(), T::class.java)
}

/**
 * Defaults to 10 minutes if null
 * @return update interval for member / download counters in milliseconds
 */
fun configUpdateInterval(): Long {
    val updateInterval = readConfigSafe<CounterConfig>(ConfigType.COUNTER, false)?.updateInterval
        ?: return TimeUnit.MINUTES.toMillis(10)
    return TimeUnit.MINUTES.toMillis(updateInterval)
}

/**
 * @return non-null bot authentication token
 */
fun getAuthToken(): String {
    return readConfigSafe<AuthConfig>(ConfigType.AUTH, false)!!.botToken
}

/**
 * Will send an error in the [message]?.channel if null.
 * @return the Github token, set in [AuthConfig]
 */
suspend fun getGithubToken(message: Message?): String? {
    val token = readConfigSafe<AuthConfig>(ConfigType.AUTH, false)?.githubToken
    if (token == null) message?.error("Github token not set in `${ConfigType.AUTH.configPath.substring(7)}`!")
    return token
}

/**
 * Will send an error in the [message]?.channel if null.
 * @return the default Github user, set in [UserConfig]
 */
suspend fun getDefaultGithubUser(message: Message?): String? {
    val repo = readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultGithubUser
    if (repo == null) message?.error("Default user / org not set in `${ConfigType.USER.configPath.substring(7)}`!")
    return repo
}

/**
 * Fake User class used when getting from Discord's API here: "https://discord.com/api/v6/users/[id]"
 * Documented here: https://discord.com/developers/docs/resources/user
 * Values are only every null if the ID doesn't exist
 */
data class FakeUser(
    val id: Long?,
    val username: String?,
    val avatar: String?,
    val discriminator: Int?,
    @SerializedName("public_flags")
    val publicFlags: Int?
)

/**
 * @return a Long. Only null if user does not exist.
 */
fun String.toUserID(server: Server): Long? {
    try {
        return this.replace("[<@!>]".toRegex(), "").toLong()
    } catch (ignored: NumberFormatException) {
        server.members.findByName(this, true)?.let {
            return it.id
        } ?: run {
            return authenticatedRequest<FakeUser>(
                "Bot",
                getAuthToken(),
                "https://discord.com/api/v6/users/$this"
            ).id
        }
    }
}
