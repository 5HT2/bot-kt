package org.kamiblue.botkt.commands

import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.channel.ServerChannel
import org.kamiblue.botkt.*
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error

object SayCommand : Command("say") {
    init {
        channel("channel") {
            bool("embed") {
                greedyString("content") {
                    doesLaterIfHas(PermissionTypes.SAY) { context ->
                        val embed: Boolean = context arg "embed"
                        val content: String = context arg "content"

                        val channel: TextChannel = (context.channelArg("channel", server) ?: run {
                            message.error("Error sending message! The text channel does not exist.")
                            return@doesLaterIfHas
                        }) as TextChannel

                        if (embed) {
                            channel.send {
                                embed {
                                    description = content
                                    color = Colors.PRIMARY.color
                                }
                            }
                        } else {
                            channel.send(content)
                        }
                    }
                }
            }
        }

        literal("edit") {
            channel("channel") {
                long("message") {
                    greedyString("content") {
                        doesLaterIfHas(PermissionTypes.SAY) { context ->
                            val channel: TextChannel = (context.channelArg("channel", server) ?: run {
                                message.error("Error sending message! The text channel does not exist.")
                                return@doesLaterIfHas
                            }) as TextChannel

                            val message: Message = channel.getMessage(context arg "message") ?: run {
                                message.error("Error editing message! The message does not exist.")
                                return@doesLaterIfHas
                            }

                            val content: String = context arg "content"

                            if (message.embeds.isNullOrEmpty()) {
                                message.edit(content)
                            } else {
                                message.edit {
                                    description = content
                                    color = Colors.PRIMARY.color
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Say or edit messages via the bot.\n\n" +
                "`$fullName <channel> <embed (true or false)> <content>`\n" +
                "`$fullName edit <channel> <message id> <new content>`"
    }
}