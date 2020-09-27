import ConfigManager.readConfigSafe
import Send.error
import com.google.gson.Gson
import net.ayataka.kordis.entity.message.Message
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * @return [T] from [url]
 */
inline fun <reified T> request(url: String): T {
    val request = Request.Builder().url(url).get().build()
    val response = OkHttpClient().newCall(request).execute()

    return Gson().fromJson(response.body()!!.string(), T::class.java)
}

/**
 * @return [T] from [url] with the [token] as the Authorization header
 */
inline fun <reified T> authenticatedRequest(token: String, url: String): T {
    val request = Request.Builder().addHeader("Authorization", "token $token").url(url).get().build()
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