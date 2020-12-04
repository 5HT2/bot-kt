package org.kamiblue.botkt.command.arguments

import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.utils.AnimatableEmoji
import org.kamiblue.botkt.utils.Emoji
import org.kamiblue.command.AbstractArg

class DiscordChannelArg(
    override val name: String
) : AbstractArg<Long>() {

    override suspend fun convertToType(string: String?): Long? {
        return string?.filter { it.isDigit() }?.toLongOrNull()
    }

}

class DiscordEmojiArg(
    override val name: String
) : AbstractArg<AnimatableEmoji>() {

    override suspend fun convertToType(string: String?): AnimatableEmoji? {
        if (string == null) return null

        val splitString = string
            .removeSurrounding("<", ">")
            .split(":")

        val animated = splitString.firstOrNull() == "a"
        val name = splitString.getOrNull(1)
        val id = splitString.getOrNull(2)?.toLongOrNull()

        return if (name == null || id == null) null
        else AnimatableEmoji(animated, Emoji(id, name))
    }

}

class DiscordUserArg(
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