package org.kamiblue.botkt.arguments

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.Main

typealias UserPromise = (suspend () -> User?)

val NO_READER_EXCEPTION = SimpleCommandExceptionType(LiteralMessage("There was no reader to read the argument."))

object DiscordUserArgumentType : ArgumentType<UserPromise> {
    override fun parse(reader: StringReader?): UserPromise {
        reader ?: throw NO_READER_EXCEPTION.create()
        val ping = reader.readDiscordPing()
        return {
            Main.client.getUser(ping)
        }
    }
}

fun StringReader.readDiscordPing(): Long {
    fun peekSkip(c: Char) {
        if (canRead() && peek() == c) skip()
    }

    peekSkip('<')
    peekSkip('@')
    peekSkip('!')

    val long = readLong()

    peekSkip('>')

    return long
}