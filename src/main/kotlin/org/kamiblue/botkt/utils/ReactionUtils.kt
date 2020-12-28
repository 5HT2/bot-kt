package org.kamiblue.botkt.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.client.request.*
import net.ayataka.kordis.DiscordClientImpl
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.entity.user.UserImpl
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.entity.Emoji
import org.kamiblue.botkt.entity.Reaction

/**
 * Add [emoji] reaction
 */
suspend fun Message.addReaction(emoji: Emoji) {
    Main.discordHttp.put<Unit> {
        url("https://discord.com/api/v8/channels/${channel.id}/messages/$id/reactions/${emoji.urlCoded}/@me")
    }
}

/**
 * Remove [emoji] reaction from bot itself
 */
suspend fun Message.removeReaction(emoji: Emoji) {
    Main.discordHttp.delete<Unit> {
        url("https://discord.com/api/v8/channels/${channel.id}/messages/$id/reactions/${emoji.urlCoded}/@me")
    }
}

/**
 * Remove [emoji] reaction from [user]
 */
suspend fun Message.removeReaction(user: User, emoji: Emoji) {
    Main.discordHttp.delete<Unit> {
        url("https://discord.com/api/v8/channels/${channel.id}/messages/$id/reactions/${emoji.urlCoded}/${user.id}")
    }
}

/**
 * Clear all [emoji] reactions
 */
suspend fun Message.clearReaction(emoji: Emoji) {
    Main.discordHttp.delete<Unit> {
        url("https://discord.com/api/v8/channels/${channel.id}/messages/$id/reactions/${emoji.urlCoded}")
    }
}

/**
 * Clear all reactions
 */
suspend fun Message.clearReaction() {
    Main.discordHttp.delete<Unit> {
        url("https://discord.com/api/v8/channels/${channel.id}/messages/$id/reactions")
    }
}

/**
 * Get a list of users that reacted to the message with [emoji]
 */
suspend fun Message.getReactions(emoji: Emoji): List<User> {
    return Main.discordHttp.get<JsonArray> {
        url("https://discord.com/api/v8/channels/${channel.id}/messages/$id/reactions/${emoji.urlCoded}")
    }.map {
        UserImpl(Main.client as DiscordClientImpl, it.asJsonObject)
    }
}

/**
 * Get a list of reactions for this message
 */
suspend fun Message.getReactions(): List<Reaction> {
    return Main.discordHttp.get<JsonObject> {
        url("https://discord.com/api/v8/channels/${channel.id}/messages/$id")
    }.getAsJsonArray("reactions").map {
        Reaction(it.asJsonObject)
    }
}
