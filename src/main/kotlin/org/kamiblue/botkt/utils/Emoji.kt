package org.kamiblue.botkt.utils

import org.kamiblue.botkt.utils.StringUtils.urlEncode


class Emoji private constructor(
    val id: Long?,
    val name: String,
    val animated: Boolean,
) {

    val isCustom = id != null

    val urlCoded: String =
        if (id == null) name.urlEncode()
        else "$name:$id".urlEncode()

    override fun toString() = when {
        animated -> "<a:$name:$id>"
        id != null -> "<:$name:$id>"
        else -> name
    }

    companion object {
        fun emoji(emoji: String) = Emoji(null, emoji, false)

        fun customEmoji(id: Long, name: String, animated: Boolean) = Emoji(id, name, animated)
    }

}