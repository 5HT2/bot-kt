package org.kamiblue.botkt.command.commands.system

import net.ayataka.kordis.entity.message.Message
import org.kamiblue.botkt.*
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.command.options.HasPermission
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.error

object SayCommand : BotCommand(
    name = "say",
    category = Category.SYSTEM,
    description = "Say or edit messages via the bot"
) {
    init {
        channel("channel") { channelArg ->
            boolean("embed") { embedArg ->
                string("title") { titleArg ->
                    greedy("content") { contentArg ->
                        execute("Say something in a channel", HasPermission.get(PermissionTypes.SAY)) {
                            val channel = channelArg.getTextChannelOrNull() ?: run {
                                message.channelError()
                                return@execute
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
                            execute("Edit an existing message in a channel", HasPermission.get(PermissionTypes.SAY)) {
                                val channel = channelArg.getTextChannelOrNull() ?: run {
                                    message.channelError()
                                    return@execute
                                }

                                val message = channel.getMessage(messageArg.value) ?: run {
                                    channel.error("Error editing message! The message ID could not be found in ${channel.id}")
                                    return@execute
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
        this.channel.error("Error finding channel! Make sure the ID / # is correct, and it is a Text or Announcement channel")
    }
}
