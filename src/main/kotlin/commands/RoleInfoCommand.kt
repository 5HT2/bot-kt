package commands

import Command
import Send.error
import arg
import doesLater
import greedyString
import long
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.role.Role
import pretty
import java.awt.Color

object RoleInfoCommand : Command("roleinfo") {
    init {
        long("id") {
            doesLater { context ->
                val id: Long = context arg "id"

                val foundRole = message.server?.roles?.find(id) ?: run {
                    message.error("Role ID not found! Does this role exist?")
                    return@doesLater
                }

                sendRoleMsg(foundRole, message)
            }
        }

        greedyString("name") {
            doesLater { context ->
                val name: String = context arg "name"

                val foundRole = message.server?.roles?.findByName(name) ?: run {
                    message.error("Role name not found! Try using the role ID.")
                    return@doesLater
                }

                sendRoleMsg(foundRole, message)
            }
        }
    }

    private suspend fun sendRoleMsg(role: Role, message: Message) {
        val prettyPermissions: String = role.permissions.pretty()

        message.channel.send {
            embed {
                title = role.name
                color = if (role.isEveryone) Colors.primary else role.color
                description = role.mention
                field("Permissions", prettyPermissions)
                field("Position", role.position.toString())
                field("Hoisted", role.hoist.toString())
            }
        }
    }

    override fun getHelpUsage(): String {
        return "`$name` <roleName/roleId>"
    }
}