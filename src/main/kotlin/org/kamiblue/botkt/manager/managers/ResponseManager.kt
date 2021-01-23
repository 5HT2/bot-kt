package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.event.events.message.MessageEditEvent
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

    private var prevConfig: ResponseConfig? = null
    private var cachedResponses = emptyList<Response>() to emptyList<Response>()

    init {
        asyncListener<MessageReceiveEvent> { event ->
            config?.let { config ->
                if (config.ignoreChannels?.contains(event.message.channel.id) == true) return@asyncListener
                val startTime = System.currentTimeMillis()
                handleResponse(config, startTime, event.message, getCachedResponse(config).first)
            }
        }

        asyncListener<MessageEditEvent> { event ->
            config?.let { config ->
                event.message?.let { message ->
                    if (config.ignoreChannels?.contains(message.channel.id) == true) return@asyncListener
                    val startTime = System.currentTimeMillis()
                    handleResponse(config, startTime, message, getCachedResponse(config).second)
                }
            }
        }
    }

    private fun getCachedResponse(config: ResponseConfig): Pair<List<Response>, List<Response>> {
        return if (config != prevConfig) {
            synchronized(this) {
                (config.responses.sortedByDescending { it.deleteMessage }
                    to config.responses.filter { it.deleteMessage })
                    .also { cachedResponses = it }
            }
        } else {
            cachedResponses
        }
    }

    private suspend fun handleResponse(config: ResponseConfig, startTime: Long, message: Message, responses: List<Response>) {
        val messageContent = message.content
        if (messageContent.isBlank()) return

        val member = message.member ?: return // Should be ignored in DM
        val channel = message.channel

        for (response in responses) {
            if (response.ignoreRoles?.isNotEmpty() == true
                && config.roleIgnorePrefix != null
                && !messageContent.startsWith(config.roleIgnorePrefix)
                && member.roles.any { response.ignoreRoles.contains(it.id) }
            ) {
                continue // If the message doesn't start with the ignore prefix and they have an ignored role, skip to the next regex
            }

            val replacedMessage = if (response.compiledWhiteLists?.isEmpty() == false) {
                var messageToReplace = messageContent
                response.compiledWhiteLists?.forEach { // Replace whitelisted words, usually used for fixing false positives
                    messageToReplace = messageToReplace.replace(it, "")
                }
                messageToReplace
            } else {
                messageContent
            }

            if (response.compiledRegexes.all { it.containsMatchIn(replacedMessage) }) {
                val stopTime = System.currentTimeMillis()
                val finalTime = stopTime - startTime // don't ask me how this is so fast

                channel.send {
                    embed {
                        title = response.responseTitle
                        description = response.responseDescription
                        color = response.color
                        footer("ID: ${member.id} | Time: ${finalTime}ms", iconUrl = member.avatar.url)
                    }
                }

                if (response.deleteMessage) {
                    message.tryDelete()
                    break // Once the message has been deleted we want to stop responding to the regexes
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
        private val whitelistReplace: List<String>?,
        val ignoreRoles: Set<Long>?,
    ) {
        val color get() = (responseColor ?: Colors.PRIMARY).color

        private var compiledWhiteListCache: List<Regex>? = null
        val compiledWhiteLists
            get() = compiledWhiteListCache ?: synchronized(this) {
                whitelistReplace?.toRegexes().also { compiledWhiteListCache = it }
            }

        private var compiledRegexCache: List<Regex>? = null
        val compiledRegexes
            get() = compiledRegexCache ?: synchronized(this) {
                regexes.toRegexes().also { compiledRegexCache = it }
            }

        private fun List<String>.toRegexes() = map {
            Main.logger.debug("Creating regex cache \"$it\" for ResponseManager")
            Regex(it, RegexOption.IGNORE_CASE)
        }
    }
}
