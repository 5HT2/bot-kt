package org.kamiblue.botkt.command

import org.kamiblue.botkt.Console
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.Permissions.missingPermissions
import org.kamiblue.botkt.utils.error
import org.kamiblue.command.ExecuteOption

class HasPermission(private val requiredPerm: PermissionTypes) : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message is Console.FakeMessage
            || event.message.author.hasPermission(requiredPerm)
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.message.missingPermissions(requiredPerm)
    }
}

object DirectOnly : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.server == null
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.channel.error("This command can only be used in direct message chat!")
    }
}

object ServerOnly : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.server != null
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.channel.error("This command can only be used in a server!")
    }
}
