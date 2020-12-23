package org.kamiblue.botkt.command

import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.event.events.message.MessageEvent
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.command.ArgIdentifier
import org.kamiblue.command.ExecuteEvent

class MessageExecuteEvent(
    args: Array<String>,
    event: MessageReceiveEvent
) : ExecuteEvent(CommandManager, args), MessageEvent by event {

    val message by event::message
    val channel = message.channel

    fun ArgIdentifier<Long>.getTextChannelOrNull(): TextChannel? =
        server?.let {
            it.textChannels.find(this.value)
                ?: it.announcementChannels.find(this.value)
        }

}