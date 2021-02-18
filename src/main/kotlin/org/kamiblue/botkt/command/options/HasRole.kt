package org.kamiblue.botkt.command.options

import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.command.execute.ExecuteOption

class HasRole(private val role: String) : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message.member?.roles?.any { it.name == role }
            ?: false
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        val role = event.server?.roles?.findByName(role, true) ?: return

        event.channel.send {
            embed {
                title = "Missing role"
                description = "You need the role `${role.name}` to use this command!"
                color = Colors.ERROR.color
            }
        }
    }
}
