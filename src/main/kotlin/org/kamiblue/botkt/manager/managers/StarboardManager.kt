package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.event.events.message.ReactionAddEvent
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.config.ServerConfigs
import org.kamiblue.botkt.config.server.StarboardConfig
import org.kamiblue.botkt.entity.Emoji
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.StringUtils.isUrl
import org.kamiblue.botkt.utils.contextLink
import org.kamiblue.botkt.utils.getReactions
import org.kamiblue.commons.extension.max
import org.kamiblue.event.listener.asyncListener

object StarboardManager : Manager {

    private val imageExtensionRegex = ".*\\.(jpg|png|gif)".toRegex()

    init {
        asyncListener<ReactionAddEvent> {
            val server = it.server ?: return@asyncListener

            if (it.reaction.emoji.name == "⭐") {
                val config = ServerConfigs.get<StarboardConfig>(server)

                if (config.channel == -1L) {
                    return@asyncListener
                }

                val channel = server.channels.find(it.reaction.channelId) as? TextChannel? ?: return@asyncListener
                val message = channel.getMessage(it.reaction.messageId) ?: return@asyncListener
                val image = message.getAttachedImageUrl()

                if (config.channel == channel.id) return@asyncListener
                if (image == null && message.content.isBlank()) return@asyncListener

                val reactionUsers = message.getReactions(Emoji("⭐"))
                Main.logger.debug("Star received, message ${message.id} has ${reactionUsers.size} star(s)")

                val starboardChannel = server.channels.find(config.channel) as? TextChannel ?: return@asyncListener

                if (reactionUsers.size >= config.threshold && !config.messages.contains(message.id)) {
                    starboardChannel.send {
                        embed {
                            description = "${message.contextLink}\n\n${message.content}".max(2048)
                            imageUrl = image
                            author(name = message.author?.tag, iconUrl = message.author?.avatar?.url)
                            timestamp = message.timestamp
                            color = Colors.STARBOARD.color
                        }
                    }
                    config.messages.add(message.id)
                }
            }
        }
    }

    private fun Message.getAttachedImageUrl(): String? {
        return attachments.firstOrNull()?.let {
            if (it.isImage) it.url
            else null
        } ?: content.run {
            if (isUrl() && matches(imageExtensionRegex)) this
            else null
        }
    }
}
