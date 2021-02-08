package org.kamiblue.botkt.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import net.ayataka.kordis.DiscordClientImpl
import net.ayataka.kordis.entity.find
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.server.permission.Permission
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.exception.MissingPermissionsException
import net.ayataka.kordis.exception.NotFoundException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kamiblue.botkt.AuthConfig
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.manager.managers.ConfigManager.readConfigSafe
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import kotlin.text.get

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
 * @return non-null bot authentication token
 */
fun getAuthToken(): String {
    return readConfigSafe<AuthConfig>(ConfigType.AUTH, false)!!.botToken
}

fun Server.maxEmojiSlots(): Int {
    val url = "https://discord.com/api/v6/guilds/${this.id}"
    val request = Request.Builder()
        .addHeader("Authorization", "Bot ${getAuthToken()}")
        .url(url).get().build()

    val response = OkHttpClient().newCall(request).execute()
    val jsonObject = response.body?.charStream()?.use {
        JsonParser.parseReader(it)
    } as? JsonObject

    val premiumTier = try {
        jsonObject?.get("premium_tier")?.asInt
    } catch (e: Exception) {
        Main.logger.warn("Error getting premium tier", e)
        0
    }

    return when (premiumTier) {
        1 -> 100
        2 -> 150
        3 -> 250
        else -> 50
    }
}

fun checkPermission(client: DiscordClientImpl, server: Server, permission: Permission) {
    val myself = server.members.find(client.botUser) ?: throw NotFoundException()

    if (isNotInitialized(myself)) {
        return
    }

    if (!myself.hasPermission(permission)) {
        throw MissingPermissionsException(server, "Permission: ${permission.desciption}")
    }
}

val notNumberRegex = Regex("[^0-9]")

// Bot users can not have one or fewer roles. If so, this means the server roles are not initialized yet.
private fun isNotInitialized(myself: Member) = myself.roles.size < 2
