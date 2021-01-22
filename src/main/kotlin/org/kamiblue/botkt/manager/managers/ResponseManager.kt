package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.ResponseConfig
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.safeDelete
import org.kamiblue.event.listener.asyncListener
import java.awt.Color

fun main() {

    for (index in 0 until 10) {
        if (index == 2) continue
        println(index)

        if (index == 5) break
    }
}

object ResponseManager : Manager {

    val config get() = ConfigManager.readConfigSafe<ResponseConfig>(ConfigType.ARCHIVE_CHANNEL, false)

    init {
        asyncListener<MessageReceiveEvent> { messageArg ->
            val config = config ?: return@asyncListener

            val messageTemp = messageArg.message.content
            if (messageTemp.isBlank()) return@asyncListener
            val channel = messageArg.message.channel

            for (response in config.responses) {
                if (response.ignoreRoles.contains(messageArg.message.author?.id)) continue // see if anything else matches

                val message = if (response.whitelistReplace.isNotEmpty()) {
                    var messageToReplace = messageTemp
                    response.whitelistReplace.forEach { replacement ->
                        messageToReplace = messageToReplace.replace(replacement, "")
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
                        messageArg.message.safeDelete()
                        break // stop auto-responding if the message has been deleted
                    }
                }
            }
        }
    }

    data class Response(
        private val regex: String,
        val compiledRegex: Regex = Regex(regex),
        val deleteMessage: Boolean,
        val responseDescription: String,
        val responseTitle: String = "",
        private val responseColor: Colors = Colors.PRIMARY,
        val whitelistReplace: HashSet<String> = hashSetOf(),
        val ignoreRoles: HashSet<Long> = hashSetOf(),
        val color: Color = responseColor.color
    )
}
