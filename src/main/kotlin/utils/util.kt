package utils

import AuthConfig
import ConfigManager.readConfigSafe
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

fun getReleaseChannel(): Long {
    val releaseChannel = readConfigSafe<UserConfig>(ConfigType.USER, false)?.downloadChannel
    if (releaseChannel == null){
        println("ERROR! Release channel not found in config! Using default channel...")
        return 743240299069046835
    }
    return releaseChannel
}

fun getUpdateInterval(): Long {
    val updateInterval = readConfigSafe<UserConfig>(ConfigType.USER, false)?.updateInterval
    if (updateInterval == null){
        println("ERROR! Update interval not found in config! Using default interval...")
        return TimeUnit.MINUTES.toMillis(10)
    }
    return TimeUnit.MINUTES.toMillis(updateInterval)
}

fun getSecondaryReleaseChannel(): Long {
    val secondaryUpdateInterval = readConfigSafe<UserConfig>(ConfigType.USER, false)?.secondaryDownloadChannel
    if (secondaryUpdateInterval == null){
        println("ERROR! Secondary download channel not found in config! Using default channel...")
        return 744072202869014571
    }
    return secondaryUpdateInterval
}

fun getServerId(): Long {
    val serverId = readConfigSafe<UserConfig>(ConfigType.USER, false)?.primaryServerId
    if (serverId == null){
        println("ERROR! Primary server ID not found in config! Using default ID...")
        return 573954110454366214
    }
    return serverId
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