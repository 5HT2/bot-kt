package org.kamiblue.botkt.command.commands

import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.channel.announcement.AnnouncementChannel
import net.ayataka.kordis.entity.server.channel.text.ServerTextChannel
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
                        executeIfHas(PermissionTypes.SAY, "Say something in a channel") {
                            var channel = channelArg.getSendableChannelOrNull() ?: run {
                                message.channelError()
                                return@executeIfHas
                            }

                            channel = server?.textChannels?.find(channel.id) ?: server?.announcementChannels?.find(channel.id) ?: run {
                                message.channelError()
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
                            executeIfHas(PermissionTypes.SAY, "Edit an existing message in a channel") {
                                var channel = channelArg.getSendableChannelOrNull() ?: run {
                                    message.channelError()
                                    return@executeIfHas
                                }

                                channel = server?.textChannels?.find(channel.id) ?: server?.announcementChannels?.find(channel.id) ?: run {
                                    message.channelError()
                                    return@executeIfHas
                                }

                                val message = (channel as? ServerTextChannel)?.getMessage(messageArg.value) ?: run {
                                    (channel as? AnnouncementChannel)?.getMessage(messageArg.value) ?: run {
                                        message.error("Error editing message! The message ID could not be found in ${channel.id}")
                                        return@executeIfHas
                                    }
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

    private suspend fun Message.channelError() {
        this.error("Error finding channel! Make sure the ID / # is correct, and it is a Text or Announcement channel")
    }
}