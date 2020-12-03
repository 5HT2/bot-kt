package org.kamiblue.botkt.command.arguments

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.utils.NO_READER_EXCEPTION

typealias UserPromise = (suspend () -> User?)

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