package org.kamiblue.botkt.command.options

import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.command.execute.ExecuteOption
import java.util.*

class AnyRole(vararg roles: String) : ExecuteOption<MessageExecuteEvent> {
    private val roles = roles.map { it.toLowerCase(Locale.ROOT) }.toHashSet()

    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message.member?.roles?.any {
            roles.contains(it.name.toLowerCase(Locale.ROOT))
        } ?: false
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        val roles = event.server?.roles?.joinToString { "`${it.name}`" }

        event.channel.send {
            embed {
                title = "Missing role"
                description = "You need one of the following roles to use this command!\n$roles"
                color = Colors.ERROR.color
            }
        }
    }
}
