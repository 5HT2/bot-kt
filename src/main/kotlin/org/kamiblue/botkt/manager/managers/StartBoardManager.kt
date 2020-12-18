package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.event.events.message.ReactionAddEvent
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.StartBoardConfig
import org.kamiblue.botkt.entity.Emoji
import org.kamiblue.botkt.event.events.ShutdownEvent
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.getReactions
import org.kamiblue.event.listener.asyncListener
import org.kamiblue.event.listener.listener
import java.awt.Color

object StartBoardManager : Manager {

    private val startBoardConfig
        get() = ConfigManager.readConfigSafe<StartBoardConfig>(ConfigType.STAR_BOARD, false)

    init {
        asyncListener<ReactionAddEvent> {
            if (it.reaction.emoji.name == "⭐") {
                val server = it.server ?: return@asyncListener
                val channel = server.channels.find(it.reaction.channelId) as? TextChannel? ?: return@asyncListener
                val message = channel.getMessage(it.reaction.messageId) ?: return@asyncListener
                val cfg = startBoardConfig ?: run {
                    Main.logger.warn("Star board config not found")
                    return@asyncListener
                }

                val reactionUsers = message.getReactions(Emoji("⭐"))
                val startBoardChannel = cfg.channels[server.id]?: run {
                    Main.logger.info("Star board channel not found for server ${server.id}")
                    return@asyncListener
                }

                Main.logger.debug("Star received, message ${message.id} now has ${reactionUsers.size} star")

                if (reactionUsers.size >= cfg.threshold && !cfg.messages.contains(message.id)) {
                    (server.channels.find(startBoardChannel) as TextChannel).send {
                        embed {
                            title = ("Message from ${message.author?.name}")
                            description = message.content
                            color = Color(255, 172, 51)
                            url = "https://discord.com/channels/${server.id}/${channel.id}/${message.id}"
                            timestamp = message.timestamp
                        }
                    }
                    cfg.messages.add(message.id)
                }
            }
        }

        listener<ShutdownEvent> {
            ConfigManager.writeConfig(ConfigType.STAR_BOARD)
        }
    }

}