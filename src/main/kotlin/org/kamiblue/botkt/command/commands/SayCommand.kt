package org.kamiblue.botkt.command.commands

import net.ayataka.kordis.entity.channel.TextChannel
import org.kamiblue.botkt.*
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error

object SayCommand : BotCommand(
    name = "say",
    description = "Say or edit messages via the bot"
) {
    init {
        channel("channel") { channelArg ->
            boolean("embed") { embedArg ->
                string("title") { titleArg ->
                    greedy("content") { contentArg ->
                        executeIfHas(PermissionTypes.SAY) {
                            val channel = channelArg.getChannelOrNull() as? TextChannel? ?: run {
                                message.error("Error sending message! The text channel does not exist.")
                                return@executeIfHas
                            }

                            if (embedArg.value) {
                                channel.send {
                                    embed {
                                        title = titleArg.value
                                        description = contentArg.value
                                        color = Colors.PRIMARY.color
                                    }
                                }
                            } else {
                                channel.send(contentArg.value)
                            }
                        }
                    }
                }
            }
        }

        literal("edit") {
            channel("channel") { channelArg ->
                long("message") { messageArg ->
                    string("title") { titleArg ->
                        greedy("content") { contentArg ->
                            executeIfHas(PermissionTypes.SAY) {
                                val channel = channelArg.getChannelOrNull() as? TextChannel? ?: run {
                                    message.error("Error sending message! The text channel does not exist.")
                                    return@executeIfHas
                                }

                                val message = channel.getMessage(messageArg.value) ?: run {
                                    message.error("Error editing message! The message does not exist.")
                                    return@executeIfHas
                                }

                                if (message.embeds.isNullOrEmpty()) {
                                    message.edit(contentArg.value)
                                } else {
                                    message.edit {
                                        title = titleArg.value
                                        description = contentArg.value
                                        color = Colors.PRIMARY.color
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}