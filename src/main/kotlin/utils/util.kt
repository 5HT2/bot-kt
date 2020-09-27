package utils

import AuthConfig
import ConfigManager.readConfigSafe
import ConfigType
import Send.error
import UserConfig
import com.google.gson.Gson
import net.ayataka.kordis.entity.message.Message
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Suppress("BlockingMethodInNonBlockingContext")
inline fun <reified T> tokenRequest(token: String, url: String): T {
    val request = Request.Builder().addHeader("Authorization", "token $token").url(url).get().build()
    val response = OkHttpClient().newCall(request).execute()

    return Gson().fromJson(response.body()!!.string(), T::class.java)
}

fun configUpdateInterval(): Long {
    val updateInterval = readConfigSafe<UserConfig>(ConfigType.USER, false)?.updateInterval
        ?: return TimeUnit.MINUTES.toMillis(10)
    return TimeUnit.MINUTES.toMillis(updateInterval)
}

suspend fun getToken(message: Message?): String? {
    val token = readConfigSafe<AuthConfig>(ConfigType.AUTH, false)?.githubToken
    if (token == null) message?.error("ERROR! Github token not found in config! Stopping...")
    return token
}

suspend fun getUser(message: Message?): String? {
    val repo = readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultGithubUser
    if (repo == null) message?.error("Default user / org not set in `${ConfigType.USER.configPath.substring(7)}`!")
    return repo
}