package org.kamiblue.botkt.manager.managers

import com.google.gson.GsonBuilder
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.ResponseConfig
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.tryDelete
import org.kamiblue.event.listener.asyncListener

fun main() {
    val gson = GsonBuilder().setPrettyPrinting().create()

    val elytraResponse = ResponseManager.Response(
        regex = "(elytra|elytra.{0,2}light|elytra.{0,2}+|elytra.{0,2}fly)",
        deleteMessage = false,
        responseDescription = "On 2b2t, Use the settings posted in <#634012886930423818>.\\n\\nOn non-2b2t servers, use Control mode with *default settings*.\\nIf it's still not working, make sure you're not using NoFall, AntiHunger or any other movement related mods from **other** clients, such as Sprint in Rage mode.",
        responseTitle = "ElytraFlight help",
        responseColor = Colors.PRIMARY
    )

    val discordResponse = ResponseManager.Response(
        regex = "(d.{0,3}.{0,3}s.{0,3}c.{0,3}.{0,3}r.{0,3}d).{0,7}(gg|com.{0,3}invite)",
        deleteMessage = true,
        responseDescription = "You don't have permission to send Discord Invites",
        responseTitle = "Rule 5",
        responseColor = Colors.ERROR,
        ignoreRoles = hashSetOf(754406610239225927)
    )

    val config = ResponseConfig(listOf(elytraResponse, discordResponse))
    println(gson.toJson(config))
}

object ResponseManager : Manager {

    val config get() = ConfigManager.readConfigSafe<ResponseConfig>(ConfigType.RESPONSE, false)

    init {
        asyncListener<MessageReceiveEvent> { event ->
            val config = config ?: return@asyncListener

            val message = event.message.content
            if (message.isBlank()) return@asyncListener
            val channel = event.message.channel

            config.responses.firstOrNull {
                it.ignoreRoles?.contains(event.message.author?.id) == false
            }?.let { response ->
                val replacedMessage = if (response.whitelistReplace != null && response.whitelistReplace.isNotEmpty()) {
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
        val whitelistReplace: List<String>? = null,
        val ignoreRoles: Set<Long>? = null,
    ) {
        val compiledRegex by lazy { Regex(regex) }
        val color get() = responseColor.color
    }
}
