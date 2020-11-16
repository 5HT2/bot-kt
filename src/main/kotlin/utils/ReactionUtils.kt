package org.kamiblue.botkt.utils

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.user.User
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.kamiblue.botkt.getAuthToken
import org.kamiblue.botkt.utils.StringUtils.uriEncode

@Suppress("UNUSED")
object ReactionUtils {
    private const val contentLength = "Content-Length"

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
}