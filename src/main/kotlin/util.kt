package org.kamiblue.botkt

import com.google.gson.Gson
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.permission.PermissionSet
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.l1ving.api.issue.Issue
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

    return Gson().fromJson(response.body!!.string(), T::class.java)
}

/**
 * [authType] is the type of header to be used. "Bot" is for Discord's API, while Github uses "token".
 * @return [T] from [url] with the [token] as the Authorization header
 */
inline fun <reified T> authenticatedRequest(authType: String, token: String, url: String): T {
    val request = Request.Builder().addHeader("Authorization", "$authType $token").url(url).get().build()
    val response = OkHttpClient().newCall(request).execute()

    return Gson().fromJson(response.body!!.string(), T::class.java)
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
 * [user] and [repo] is the user/repo you want to create the issue in.
 * [token] is a Github Token with repo:public_repo checked
 */
fun createGithubIssue(issue: Issue, user: String, repo: String, token: String) {
    val url = "https://api.github.com/repos/$user/$repo/issues"
    val body = Gson().toJson(issue).toRequestBody("".toMediaTypeOrNull())

    val request = Request.Builder()
        .addHeader("Accept", "application/vnd.github.v3+json")
        .addHeader("Authorization", "token $token")
        .url(url).post(body).build()

    val response = OkHttpClient().newCall(request).execute()

    println(response.body?.string())
}