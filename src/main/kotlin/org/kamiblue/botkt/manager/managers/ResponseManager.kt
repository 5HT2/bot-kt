package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.ResponseConfig
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.tryDelete
import org.kamiblue.event.listener.asyncListener

object ResponseManager : Manager {

    val config get() = ConfigManager.readConfigSafe<ResponseConfig>(ConfigType.ARCHIVE_CHANNEL, false)

    init {
        asyncListener<MessageReceiveEvent> { event ->
            val config = config ?: return@asyncListener

            val message = event.message.content
            if (message.isBlank()) return@asyncListener
            val channel = event.message.channel

            config.responses.firstOrNull {
                !it.ignoreRoles.contains(event.message.author?.id)
            }?.let { response ->
                val replacedMessage = if (response.whitelistReplace.isNotEmpty()) {
                    var messageToReplace = message
                    response.whitelistReplace.forEach {
                        messageToReplace = messageToReplace.replace(it, "")
                    }
                    messageToReplace
                } else {
                    message
                }

                if (response.compiledRegex.containsMatchIn(replacedMessage)) {
                    channel.send {
                        embed {
                            title = response.responseTitle
                            description = response.responseDescription
                            color = response.color
                        }
                    }

                    if (response.deleteMessage) {
                        event.message.tryDelete()
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
