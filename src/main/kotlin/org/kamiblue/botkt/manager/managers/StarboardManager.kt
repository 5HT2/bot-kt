package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.event.events.message.ReactionAddEvent
import org.kamiblue.botkt.BackgroundScope
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.StarBoardConfig
import org.kamiblue.botkt.entity.Emoji
import org.kamiblue.botkt.event.events.ShutdownEvent
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.StringUtils.isUrl
import org.kamiblue.botkt.utils.contextLink
import org.kamiblue.botkt.utils.getReactions
import org.kamiblue.commons.extension.max
import org.kamiblue.event.listener.asyncListener
import org.kamiblue.event.listener.listener

object StarboardManager : Manager {

    private val starBoard
        get() = ConfigManager.readConfigSafe<StarBoardConfig>(ConfigType.STAR_BOARD, false)

    private val imageExtensionRegex = ".*\\.(jpg|png|gif)".toRegex()

    init {
        asyncListener<ReactionAddEvent> {
            if (it.reaction.emoji.name == "⭐") {
                val server = it.server ?: return@asyncListener
                val channel = server.channels.find(it.reaction.channelId) as? TextChannel? ?: return@asyncListener
                val message = channel.getMessage(it.reaction.messageId) ?: return@asyncListener
                val image = message.getAttachedImageUrl()
                val cfg = starBoard ?: run {
                    Main.logger.warn("Starboard config not found")
                    return@asyncListener
                }

                val starBoardChannel = cfg.channels[server.id] ?: run {
                    Main.logger.info("Starboard channel not found for server ${server.id}")
                    return@asyncListener
                }

                if (starBoardChannel == channel.id) return@asyncListener
                if (image == null && message.content.isBlank()) return@asyncListener

                val reactionUsers = message.getReactions(Emoji("⭐"))
                Main.logger.debug("Star received, message ${message.id} now has ${reactionUsers.size} star")

                if (reactionUsers.size >= cfg.threshold && !cfg.messages.contains(message.id)) {
                    (server.channels.find(starBoardChannel) as? TextChannel)?.send {
                        embed {
                            description = "${message.contextLink}\n\n${message.content}".max(2048)
                            imageUrl = image
                            author(name = message.author?.tag, iconUrl = message.author?.avatar?.url)
                            timestamp = message.timestamp
                            color = Colors.STARBOARD.color
                        }
                    }
                    cfg.messages.add(message.id)
                }
            }
        }

        listener<ShutdownEvent> {
            ConfigManager.writeConfig(ConfigType.STAR_BOARD)
        }

        BackgroundScope.launchLooping("Star board config", 300000L) {
            ConfigManager.writeConfig(ConfigType.STAR_BOARD)
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
