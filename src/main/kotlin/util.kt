package org.kamiblue.botkt

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.user.User
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.Send.error
import org.kamiblue.botkt.Send.log
import org.kamiblue.botkt.helpers.StringHelper.toHumanReadable
import org.kamiblue.botkt.helpers.StringHelper.uriEncode
import org.l1ving.api.issue.Issue
import java.io.PrintWriter
import java.io.StringWriter
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

    return Gson().fromJson(response.body?.string(), T::class.java)
}

/**
 * [authType] is the type of header to be used. "Bot" is for Discord's API, while Github uses "token".
 * @return [T] from [url] with the [token] as the Authorization header
 */
inline fun <reified T> authenticatedRequest(authType: String, token: String, url: String): T {
    val request = Request.Builder().addHeader("Authorization", "$authType $token").url(url).get().build()
    val response = OkHttpClient().newCall(request).execute()

    return Gson().fromJson(response.body?.string(), T::class.java)
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
    val finalEmoji = if (encode) emoji.toString().uriEncode() else emoji.toString()

    val url = "https://discord.com/api/v6/channels/${this.channel.id}/messages/${this.id}/reactions/$finalEmoji/@me"
    val body = "".toRequestBody("".toMediaTypeOrNull())

    val request = Request.Builder()
        .addHeader(contentLength, "0")
        .addHeader("Authorization", "Bot ${getAuthToken()}")
        .url(url).put(body).build()

    OkHttpClient().newCall(request).execute()
}

/**
 * [allReactions] will remove all reactions on a message
 * [userID] will remove only reactions for a specific user when used with [emoji]
 * [emoji] will specify which emoji to remove for a user, or if [allReactions] is true, all reactions with that emoji.
 * If [userID] is null and [emoji] is not null, this will remove the bots own reaction with that emoji.
 * [encode] is if you want to URI encode your [emoji]
 */
fun Message.removeReactions(allReactions: Boolean = true, userID: Long? = null, emoji: Char? = null, encode: Boolean = true) {
    val finalEmoji = if (emoji == null) null else if (encode) emoji.toString().uriEncode() else emoji.toString()

    val reaction = if (allReactions && finalEmoji == null) "" // delete all reactions
    else if (allReactions && finalEmoji != null) "/$finalEmoji" // delete all reactions for a specific emoji
    else if (userID == null && finalEmoji != null) "/$finalEmoji/@me" // delete own reaction for an emoji
    else if (userID != null && finalEmoji != null) "/$finalEmoji/$userID" // delete user reaction for an emoji
    else "" // this should be equivalent to all reactions. this should only happen with user error

    val url = "https://discord.com/api/v6/channels/${this.channel.id}/messages/${this.id}/reactions$reaction"

    val request = Request.Builder()
        .addHeader(contentLength, "0")
        .addHeader("Authorization", "Bot ${getAuthToken()}")
        .url(url).delete().build()

    OkHttpClient().newCall(request).execute()
}

/**
 * [emoji] is the emoji you want to return the reactions for
 * [encode] is if you want to URI encode your [emoji]
 */
fun Message.getReactions(emoji: Char, encode: Boolean = true): List<User>? {
    val finalEmoji = if (encode) emoji.toString().uriEncode() else emoji.toString()

    val url = "https://discord.com/api/v6/channels/${this.channel.id}/messages/${this.id}/reactions/$finalEmoji"

    val request = Request.Builder()
        .addHeader(contentLength, "0")
        .addHeader("Authorization", "Bot ${getAuthToken()}")
        .url(url).get().build()

    val response = OkHttpClient().newCall(request).execute()

    return try {
        Gson().fromJson(response.body?.string(), object : TypeToken<List<FakeUser>>() {}.type)
    } catch (e: Exception) {
        null
    }
}

fun Message.getReactions(): List<FakeReaction>? {
    val url = "https://discord.com/api/v6/channels/${this.channel.id}/messages/${this.id}"

    val request = Request.Builder()
        .addHeader(contentLength, "0")
        .addHeader("Authorization", "Bot ${getAuthToken()}")
        .url(url).get().build()

    val response = OkHttpClient().newCall(request).execute()

    val jsonObject = Gson().fromJson(response.body?.string(), Any::class.java) as LinkedTreeMap<*, *>
    val reactions = jsonObject["reactions"]

    return try {
        Gson().fromJson(reactions.toString(), object : TypeToken<List<FakeReaction>>() {}.type)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
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
    // TODO: Return response
}

fun Server?.maxEmojiSlots(): Int {
    val server = this ?: return 50
    val url = "https://discord.com/api/v6/guilds/${server.id}"
    val request = Request.Builder()
        .addHeader("Authorization", "Bot ${getAuthToken()}")
        .url(url).get().build()

    val response = OkHttpClient().newCall(request).execute()
    val guildObject = Gson().fromJson(response.body?.string(), Any::class.java) as LinkedTreeMap<*, *>

    val premiumTierObject = guildObject["premium_tier"] ?: run {
        log("Error getting premium tier")
        return 50
    }

    val premiumTier = try {
        (premiumTierObject as Double).toInt()
    } catch (e: NumberFormatException) {
        0
    }

    return when (premiumTier) {
        1 -> 100
        2 -> 150
        3 -> 250
        else -> 50
    }
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
    val publicFlags: Int,
    val bot: Boolean
)

data class FakeReaction(
    val emoji: Emoji,
    val count: Int,
    @SerializedName("me")
    val selfReacted: Boolean
)

data class Emoji(
    val id: Long,
    val name: String
)

private const val contentLength = "Content-Length"

inline fun <reified T> Any?.maybeCast(): T? = this as? T