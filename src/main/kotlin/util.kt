import ConfigManager.readConfigSafe
import Send.error
import com.google.gson.Gson
import net.ayataka.kordis.entity.message.Message
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * @return [T] from [url] with the [token] as the Authorization header
 */
inline fun <reified T> authenticatedRequest(token: String, url: String): T {
    val request = Request.Builder().addHeader("Authorization", "token $token").url(url).get().build()
    val response = OkHttpClient().newCall(request).execute()

    return Gson().fromJson(response.body()!!.string(), T::class.java)
}

/**
 * @return update interval for member / download counters
 * Defaults to 10 minutes if null
 */
fun configUpdateInterval(): Long {
    val updateInterval = readConfigSafe<UserConfig>(ConfigType.USER, false)?.counterUpdateInterval
        ?: return TimeUnit.MINUTES.toMillis(10)
    return TimeUnit.MINUTES.toMillis(updateInterval)
}

/**
 * @return the Github token, set in [AuthConfig]
 * Will send an error in the [message]?.channel if null.
 */
suspend fun getGithubToken(message: Message?): String? {
    val token = readConfigSafe<AuthConfig>(ConfigType.AUTH, false)?.githubToken
    if (token == null) message?.error("Github token not set in `${ConfigType.AUTH.configPath.substring(7)}`!")
    return token
}

/**
 * @return the default Github user, set in [UserConfig]
 * Will send an error in the [message]?.channel if null.
 */
suspend fun getDefaultGithubUser(message: Message?): String? {
    val repo = readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultGithubUser
    if (repo == null) message?.error("Default user / org not set in `${ConfigType.USER.configPath.substring(7)}`!")
    return repo
}