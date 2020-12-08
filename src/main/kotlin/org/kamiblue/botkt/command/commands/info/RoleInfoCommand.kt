package org.kamiblue.botkt.command.commands.info

import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.role.Role
import org.kamiblue.botkt.*
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.pretty

object RoleInfoCommand : BotCommand(
    name = "roleinfo",
    category = Category.INFO,
    description = "Get information about a role"
) {
    init {
        long("id") { idArg ->
            execute {
                val id = idArg.value

                val role = message.server?.roles?.find(id) ?: run {
                    message.error("Role ID not found! Does this role exist?")
                    return@execute
                }

                sendRoleMsg(role, message)
            }
        }

        greedy("name") { nameArg ->
            execute {
                val name = nameArg.value

                val role = message.server?.roles?.findByName(name) ?: run {
                    message.error("Role name not found! Try using the role ID.")
                    return@execute
                }

                sendRoleMsg(role, message)
            }
        }
    }

    private suspend fun sendRoleMsg(role: Role, message: Message) {
        val prettyPermissions: String = role.permissions.pretty()

        message.channel.send {
            embed {
                title = role.name
                color = if (role.isEveryone) Colors.PRIMARY.color else role.color
                description = role.mention
                field("Permissions", prettyPermissions)
                field("Position", role.position.toString())
                field("Hoisted", role.hoist.toString())
            }
        }
    }

}