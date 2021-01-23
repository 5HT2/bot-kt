package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.ResponseConfig
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.tryDelete
import org.kamiblue.event.listener.asyncListener

object ResponseManager : Manager {

    private val config get() = ConfigManager.readConfigSafe<ResponseConfig>(ConfigType.RESPONSE, false)

    init {
        asyncListener<MessageReceiveEvent> { event ->
            val config = config ?: return@asyncListener

            val message = event.message.content
            if (message.isBlank()) return@asyncListener
            val channel = event.message.channel

            for (response in config.responses) {
                response.ignoreRoles?.let {
                    if (it.isNotEmpty() && it.any { responseRole -> event.message.member?.roles?.any { role -> role.id == responseRole } == true }) {
                        return@asyncListener
                    }
                }

                val replacedMessage = if (response.whitelistReplace != null && response.whitelistReplace.isNotEmpty()) {
                    var messageToReplace = message
                    response.whitelistReplace.forEach {
                        messageToReplace = messageToReplace.replace(it, "")
                    }
                    messageToReplace
                } else {
                    message
                }

                if (response.compiledRegexes.all { it.containsMatchIn(replacedMessage) }) {
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
        val responseTitle: String,
        val responseDescription: String,
        private val responseColor: Colors?,
        val deleteMessage: Boolean,
        private val regexes: List<String>,
        val whitelistReplace: List<String>?,
        val ignoreRoles: Set<Long>?,
    ) {
        val color get() = (responseColor ?: Colors.PRIMARY).color

        private var compiledRegexCache: List<Regex>? = null
        val compiledRegexes
            get() = compiledRegexCache ?: synchronized(this) {
                regexes.toRegex().also { compiledRegexCache = it }
            }
    }

    private fun List<String>.toRegex(): List<Regex> {
        val regexes = arrayListOf<Regex>()
        this.forEach {
            Main.logger.debug("Creating regex cache \"$it\" for ResponseManager")
            regexes.add(Regex(it, RegexOption.IGNORE_CASE))
        }
        return regexes
    }
}
