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
val NOT_IN_PING_FORMAT_EXCEPTION = DynamicCommandExceptionType { LiteralMessage("The string '$it' is not in ping format.") }

object DiscordUserArgumentType : ArgumentType<UserPromise> {
    override fun parse(reader: StringReader?): UserPromise {
        val reader = reader ?: throw NO_READER_EXCEPTION.create()
        val str = reader.readString()
        val idStr = str.removePrefix("<@!").removePrefix("<@").removeSuffix(">")
        val id = idStr.toLongOrNull() ?: throw NOT_IN_PING_FORMAT_EXCEPTION.create(str)
        return {
            Main.client?.getUser(id)
        }
    }
}