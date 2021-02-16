package org.kamiblue.botkt.command.options

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.command.execute.ExecuteOption

object IgnoreSelf : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message.author?.id != Main.client.botUser.id
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        // Don't do anything
    }
}
