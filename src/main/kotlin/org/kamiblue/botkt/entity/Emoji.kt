package org.kamiblue.botkt.entity

import com.google.gson.JsonObject
import net.ayataka.kordis.entity.server.emoji.PartialEmoji
import net.ayataka.kordis.entity.server.emoji.PartialEmojiImpl
import net.ayataka.kordis.utils.asLongOrNull
import net.ayataka.kordis.utils.getOrNull
import org.kamiblue.botkt.utils.StringUtils.urlEncode


class Emoji(
    val id: Long?,
    val name: String,
    val animated: Boolean,
) {

    constructor(emoji: String) : this(null, emoji, false)

    val isCustom = id != null

    val urlCoded: String =
        if (id == null) name.urlEncode()
        else "$name:$id".urlEncode()

    override fun toString() = when {
        animated -> "<a:$name:$id>"
        id != null -> "<:$name:$id>"
        else -> name
    }

}

fun partialEmoji(jsonObject: JsonObject): PartialEmoji =
    PartialEmojiImpl(
        jsonObject.getOrNull("id")?.asLongOrNull,
        jsonObject.get("name").asString
    )