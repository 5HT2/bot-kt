package org.kamiblue.botkt.command.options

import org.kamiblue.botkt.Console
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.command.execute.ExecuteOption

class AnyPermission(private vararg val permissions: PermissionTypes) : ExecuteOption<MessageExecuteEvent> {
    private val permissionListString = permissions.joinToString { "`${it.name.toHumanReadable()}`" }

    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message is Console.FakeMessage ||
            permissions.any { event.message.author.hasPermission(it) }
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.channel.send {
            embed {
                title = "Missing permission"
                description = "You need one of the following permissions to use this command!\n$permissionListString"
                color = Colors.ERROR.color
            }
        }
    }
}
