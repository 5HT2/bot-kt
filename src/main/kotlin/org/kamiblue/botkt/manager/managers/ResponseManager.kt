package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.member.Member
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

    init {
        asyncListener<MessageReceiveEvent> { event ->
            config?.let { config ->
                val startTime = System.currentTimeMillis()
                handleResponse(startTime, event.message, config.responses.sort())
            }
        }

        asyncListener<MessageEditEvent> { event ->
            config?.let { config ->
                event.message?.let { message ->
                    val startTime = System.currentTimeMillis()
                    handleResponse(startTime, message, config.responses.filter { it.deleteMessage })
                }
            }
        }
    }

    private suspend fun handleResponse(startTime: Long, message: Message, responses: List<Response>) {
        val config = config ?: return

        val messageContent = message.content
        if (messageContent.isBlank()) return

        val member = message.member
        val channel = message.channel

        for (response in responses) {
            val roles = response.ignoreRoles
            if (roles?.isNotEmpty() == true && !messageContent.startsWith(config.roleIgnorePrefix) && roles.findIgnoredRole(member)) {
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
                val finalTime = (startTime - stopTime).coerceAtLeast(1) // don't ask me how this is so fast

                channel.send {
                    embed {
                        title = response.responseTitle
                        description = response.responseDescription
                        color = response.color
                        footer("ID: ${member?.id} | Time: ${finalTime}ms", iconUrl = member?.avatar?.url)
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
                whitelistReplace?.toRegex().also { compiledWhiteListCache = it }
            }

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

    // Magic sort method to get responses with the delete Boolean first
    private fun List<Response>.sort(): List<Response> {
        val array = this.toTypedArray()
        array.sortWith { response1, response2 ->
            response2.deleteMessage.compareTo(response1.deleteMessage)
        }
        return array.toList()
    }

    // Don't ask me how this works, it's stupid
    private fun Set<Long>.findIgnoredRole(member: Member?): Boolean {
        return this.any { responseRole -> member?.roles?.any { role -> role.id == responseRole } == true }
    }
}
