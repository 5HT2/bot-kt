package org.kamiblue.botkt.command.options

import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.error
import org.kamiblue.command.execute.ExecuteOption

object ServerOnly : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.server != null
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.channel.error("This command can only be used in a server!")
    }
}
