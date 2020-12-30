package org.kamiblue.botkt.command.arguments

import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.entity.Emoji
import org.kamiblue.command.AbstractArg

class ChannelArg(
    override val name: String
) : AbstractArg<Long>() {

    override suspend fun convertToType(string: String?): Long? {
        return string?.filter { it.isDigit() }?.toLongOrNull()
    }
}

class EmojiArg(
    override val name: String
) : AbstractArg<Emoji>() {

    override suspend fun convertToType(string: String?): Emoji? {
        string ?: return null

        if (string.matches(emojiRegex)) {
            return Emoji(string)
        }

        val splitString = string
            .removeSurrounding("<", ">")
            .split(":")

        val animated = splitString.firstOrNull() == "a"
        val name = splitString.getOrNull(1)
        val id = splitString.getOrNull(2)?.toLongOrNull()

        return if (name == null || id == null) null
        else Emoji(id, name, animated)
    }

    private companion object {
        val emojiRegex = "([\\u2000-\\u3300]|[\\uD83C\\uD000-\\uD83C\\uDFFF]|[\\uD83D\\uD000-\\uD83D\\uDFFF]|[\\uD83E\\uD000-\\uD83E\\uDFFF])+".toRegex()
    }
}

class UserArg(
    override val name: String
) : AbstractArg<User>() {

    override suspend fun convertToType(string: String?): User? {
        val id = string
            ?.filter { it.isDigit() }
            ?.toLongOrNull()
            ?: return null

        return Main.client.getUser(id)
    }
}
