package org.kamiblue.botkt.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import org.kamiblue.botkt.utils.NO_READER_EXCEPTION

object DiscordChannelArgumentType : ArgumentType<Long> {
    override fun parse(reader: StringReader?): Long {
        reader ?: throw NO_READER_EXCEPTION.create()
        @Suppress("UnnecessaryVariable") // it lies, will throw exception otherwise, you need to wait for reader to finish
        val channel = reader.readDiscordChannel()
        return channel
    }
}

fun StringReader.readDiscordChannel(): Long {
    fun peekSkip(c: Char) {
        if (canRead() && peek() == c) skip()
    }

    peekSkip('<')
    peekSkip('#')

    val long = readLong()

    peekSkip('>')

    return long
}