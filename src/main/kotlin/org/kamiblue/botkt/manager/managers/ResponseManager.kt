package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.ResponseConfig
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.safeDelete
import org.kamiblue.event.listener.asyncListener

object ResponseManager : Manager {

    val config get() = ConfigManager.readConfigSafe<ResponseConfig>(ConfigType.ARCHIVE_CHANNEL, false)

    init {
        asyncListener<MessageReceiveEvent> { event ->
            val config = config ?: return@asyncListener

            val messageTemp = event.message.content
            if (messageTemp.isBlank()) return@asyncListener
            val channel = event.message.channel

            for (response in config.responses) {
                if (response.ignoreRoles.contains(event.message.author?.id)) continue // see if anything else matches

                val message = if (response.whitelistReplace.isNotEmpty()) {
                    var messageToReplace = messageTemp
                    response.whitelistReplace.forEach {
                        messageToReplace = messageToReplace.replace(it, "")
                    }
                    messageToReplace
                } else {
                    messageTemp
                }

                if (response.compiledRegex.containsMatchIn(message)) {
                    channel.send {
                        embed {
                            title = response.responseTitle
                            description = response.responseDescription
                            color = response.color
                        }
                    }

                    if (response.deleteMessage) {
                        event.message.safeDelete()
                        break // stop auto-responding if the message has been deleted
                    }
                }
            }
        }
    }

    class Response(
        val responseTitle: String = "",
        val responseDescription: String,
        private val responseColor: Colors = Colors.PRIMARY,
        val deleteMessage: Boolean,
        private val regex: String,
        val whitelistReplace: List<String> = emptyList(),
        val ignoreRoles: Set<Long> = emptySet(),
    ) {
        val compiledRegex by lazy { Regex(regex) }
        val color get()= responseColor.color
    }
}
