package org.kamiblue.botkt.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import org.kamiblue.botkt.utils.AnimatableEmoji
import org.kamiblue.botkt.utils.Emoji
import org.kamiblue.botkt.utils.NO_READER_EXCEPTION

object DiscordEmojiArgumentType : ArgumentType<AnimatableEmoji> {
    override fun parse(reader: StringReader?): AnimatableEmoji {
        reader ?: throw NO_READER_EXCEPTION.create()
        return reader.readDiscordEmoji()
    }
}

fun StringReader.readDiscordEmoji(): AnimatableEmoji {
    var animated = false

    fun peekSkip(c: Char) {
        if (canRead() && peek() == c) skip()
    }

    peekSkip('<')

    if (canRead() && peek() == 'a') { // animated emojis have an 'a' added in <a:name:id>
        animated = true
        skip()
    }

    peekSkip(':')
    val name = readString()
    peekSkip(':')
    val id = readLong()
    peekSkip('>')

    return AnimatableEmoji(animated, Emoji(id, name))
}
