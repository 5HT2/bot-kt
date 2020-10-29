import ConfigManager.readConfigSafe
import Send.error
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import helpers.StringHelper.toHumanReadable
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.permission.PermissionSet
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.l1ving.api.issue.Issue
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLEncoder
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
 * [emoji] is the single unicode Char you want to react with
 * [encode] is if you want to URI encode your [emoji]
 */
fun Message.addReaction(emoji: Char, encode: Boolean = true) {
    val encodedEmoji = URLEncoder.encode(emoji.toString(), "utf-8")
    val url = "https://discord.com/api/v6/channels/${this.channel.id}/messages/${this.id}/reactions/${if (encode) encodedEmoji else emoji.toString()}/@me"
    val body = RequestBody.create(MediaType.parse(""), "")

    val request = Request.Builder()
        .addHeader("Content-Length", "0")
        .addHeader("Authorization", "Bot ${getAuthToken()}")
        .url(url).put(body).build()

    OkHttpClient().newCall(request).execute()
}

/**
 * [user] and [repo] is the user/repo you want to create the issue in.
 * [token] is a Github Token with repo:public_repo checked
 */
fun createGithubIssue(issue: Issue, user: String, repo: String, token: String) {
    val url = "https://api.github.com/repos/$user/$repo/issues"
    val body = RequestBody.create(MediaType.parse(""), Gson().toJson(issue))

    val request = Request.Builder()
        .addHeader("Accept", "application/vnd.github.v3+json")
        .addHeader("Authorization", "token $token")
        .url(url).post(body).build()

    val response = OkHttpClient().newCall(request).execute()

    println(response.body()?.string())
}

fun Exception.getStackTraceAsString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    this.printStackTrace(pw)
    return sw.toString()
}

data class FakeUser(
    val id: Long,
    val username: String,
    val avatar: String,
    val discriminator: Int,
    @SerializedName("public_flags")
    val publicFlags: Int
)
