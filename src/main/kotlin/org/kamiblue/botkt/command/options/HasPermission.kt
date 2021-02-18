package org.kamiblue.botkt.command.options

import org.kamiblue.botkt.Console
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.command.execute.ExecuteOption
import java.util.*

class HasPermission private constructor(private val permission: PermissionTypes) : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message is Console.FakeMessage
            || event.message.author.hasPermission(permission)
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.channel.send {
            embed {
                title = "Missing permission"
                description = "You need the permission `${permission.name.toHumanReadable()}` to use this command!"
                color = Colors.ERROR.color
            }
        }
    }

    companion object {
        private val cached = EnumMap<PermissionTypes, HasPermission>(PermissionTypes::class.java)

        fun get(permission: PermissionTypes): HasPermission = cached.getOrPut(permission) {
            HasPermission(permission)
        }
    }
}
