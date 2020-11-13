package helpers

import Main
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.ayataka.kordis.entity.user.User

typealias UserPromise = (suspend () -> User?)

val NO_READER_EXCEPTION = SimpleCommandExceptionType(LiteralMessage("There was no reader to read the argument."))
val NOT_IN_PING_FORMAT_EXCEPTION = DynamicCommandExceptionType { LiteralMessage("The string `$it` is not in ping format.") }

object DiscordUserArgumentType : ArgumentType<UserPromise> {
    override fun parse(reader: StringReader?): UserPromise {
        reader ?: throw NO_READER_EXCEPTION.create()
        val stringReader = UserStringReader(reader)
        val str = stringReader.readString()
//        val idStr = str.replace(Regex("^[<@!]{0,3}"), "").replace(Regex(">$"), "")
//        val idStr = str.removePrefix("<@!").removePrefix("<@").removeSuffix(">")
        val id = str.toLongOrNull() ?: throw NOT_IN_PING_FORMAT_EXCEPTION.create(str)
        return {
            Main.client?.getUser(id)
        }
    }
}

class UserStringReader(stringReader: StringReader?) : StringReader(stringReader) {
    override fun readString(): String {
        if (!canRead()) {
            return ""
        }

        return readUnquotedString()
    }

    override fun readUnquotedString(): String {
        val start = cursor
        while (canRead() && isAllowedInString(peek())) {
            skip()
        }
        return string.substring(start, cursor)
    }

    private fun isAllowedInString(c: Char) = c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z'
            || c == '_' || c == '-' || c == '.' || c == '+' || c == '<' || c == '>' || c == '@' || c == '!' || c == '#' || c == '&'
}