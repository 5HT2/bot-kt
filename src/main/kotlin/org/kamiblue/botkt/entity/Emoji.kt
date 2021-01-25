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
        if (id == null) name.urlEncode().removeVariationSelector()
        else "$name:$id".urlEncode().removeVariationSelector()

    override fun toString() = when {
        animated -> "<a:$name:$id>"
        id != null -> "<:$name:$id>"
        else -> name
    }
}

// Discord does not like this
private fun String.removeVariationSelector() = this.replace("%EF%B8%8F", "")

fun partialEmoji(jsonObject: JsonObject): PartialEmoji =
    PartialEmojiImpl(
        jsonObject.getOrNull("id")?.asLongOrNull,
        jsonObject.get("name").asString
    )
