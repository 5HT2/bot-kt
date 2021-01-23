package org.kamiblue.botkt.manager.managers

import com.google.gson.GsonBuilder
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.ResponseConfig
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.tryDelete
import org.kamiblue.event.listener.asyncListener

fun main() {
    val gson = GsonBuilder().setPrettyPrinting().create()

    val elytraResponse = ResponseManager.Response(
        regexes = listOf(".*(elytra|elytra.{0,2}light|elytra.{0,2}+|elytra.{0,2}fly).*", "(settings|config|configure)"),
        deleteMessage = false,
        responseDescription = "On 2b2t, Use the settings posted in <#634012886930423818>.\\n\\nOn non-2b2t servers, use Control mode with *default settings*.\\nIf it's still not working, make sure you're not using NoFall, AntiHunger or any other movement related mods from **other** clients, such as Sprint in Rage mode.",
        responseTitle = "ElytraFlight help",
        responseColor = Colors.PRIMARY,
        whitelistReplace = null,
        ignoreRoles = null
    )

    val discordResponse = ResponseManager.Response(
        regexes = listOf("(d.{0,3}.{0,3}s.{0,3}c.{0,3}.{0,3}r.{0,3}d).{0,7}(gg|com.{0,3}invite)"),
        deleteMessage = true,
        responseDescription = "You don't have permission to send Discord Invites",
        responseTitle = "Rule 5",
        responseColor = Colors.ERROR,
        ignoreRoles = hashSetOf(754406610239225927),
        whitelistReplace = null
    )

    val config = ResponseConfig(listOf(elytraResponse, discordResponse))
    println(gson.toJson(config))
}

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
                        break // Once the message has been deleted we want to stop responding to the regexes
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
