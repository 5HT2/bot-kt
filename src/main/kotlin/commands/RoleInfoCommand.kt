package commands

import Command
import Send.error
import Send.warn
import arg
import doesLater
import greedyString
import helpers.StringHelper.toHumanReadable
import long
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.role.Role

object RoleInfoCommand : Command("roleinfo") {
    init {
        greedyString("roleName") {
            doesLater {context ->
                val roleName: String = context arg "roleName"
                val foundRole = message.server?.roles?.findByName(roleName) ?: run {
                    message.error("Role name not found! Try using the role id.")
                    return@doesLater
                }
                sendRoleMsg(foundRole, message)
            }
        }
        long("roleId") {
            doesLater { context ->
                val roleId: Long = context arg "roleId"
                val foundRole = message.server?.roles?.find(roleId) ?: run {
                    message.error("Role name not found! Does this role exist?")
                    return@doesLater
                }
                sendRoleMsg(foundRole, message)
            }
        }
    }

    private suspend fun sendRoleMsg(foundRole: Role, message: Message) {
        if (foundRole.isEveryone) message.warn("Everyone role")
        // someone make a regex
        val filteredPermission: String = foundRole.permissions
            .toString()
            .toLowerCase()
            .toHumanReadable()
            .replace("Permissionset", "")
            .replace("(" ,"")
            .replace(")", "")

        message.channel.send {
            embed {
                title = foundRole.name
                color = foundRole.color
                field("Permissions", filteredPermission)
                field("Separate from online members?", foundRole.hoist.toString())
                field("Position", foundRole.position.toString())
                field("Mention", foundRole.mention)
            }
        }
    }

    override fun getHelpUsage(): String {
        return "`$name` <roleName/roleId>"
    }
}