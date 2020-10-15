package commands

import Command
import Send.error
import Send.warn
import arg
import commands.ChannelCommand.pretty
import doesLaterIfHas
import greedyString
import helpers.StringHelper.toHumanReadable
import long
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.role.Role

object RoleInfoCommand : Command("roleinfo") {
    init {
        greedyString("roleName") {
            doesLaterIfHas(PermissionTypes.COUNCIL_MEMBER) { context ->
                val roleName: String = context arg "roleName"
                val foundRole = message.server?.roles?.findByName(roleName) ?: run {
                    message.error("Role name not found! Try using the role ID.")
                    return@doesLaterIfHas
                }
                sendRoleMsg(foundRole, message)
            }
        }
        long("roleId") {
            doesLaterIfHas(PermissionTypes.COUNCIL_MEMBER) { context ->
                val roleID: Long = context arg "roleID"
                val foundRole = message.server?.roles?.find(roleID) ?: run {
                    message.error("Role name not found! Does this role exist?")
                    return@doesLaterIfHas
                }
                sendRoleMsg(foundRole, message)
            }
        }
    }

    private suspend fun sendRoleMsg(foundRole: Role, message: Message) {
        val filteredPermission: String = foundRole.permissions.pretty()

        message.channel.send {
            embed {
                title = foundRole.name
                color = foundRole.color
                field("Mention", foundRole.mention)
                field("Permissions", "$filteredPermission ${if (foundRole.hoist) ", Separate From Online Members" else ""}")
                field("Position", foundRole.position.toString())
            }
        }
    }

    override fun getHelpUsage(): String {
        return "`$name` <roleName/roleId>"
    }
}