package commands

import Command
import Main
import doesLater
import net.ayataka.kordis.entity.edit
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.permission.overwrite.RolePermissionOverwrite
import net.ayataka.kordis.entity.server.permission.overwrite.UserPermissionOverwrite

// TODO: make utils for permissions because this is all going to become very boilerplate
object ArchiveCommand : Command("archive") {
    init {
        doesLater { context ->
            if (message.author?.id != 563138570953687061) {
                message.channel.send {
                    embed {
                        field("Error", "You don't have permission to use this command!", true)
                        color = Main.Colors.WARN.color
                    }
                }
                return@doesLater
            }

            val channel = server?.channels?.find(message.channel.id)
            val archivedChannelsNum = server?.channels?.filter { c -> c.name.contains(Regex("archived")) }?.size

            val userOverrides: Collection<UserPermissionOverwrite>? =
                server?.channels?.find(channel!!.id)?.userPermissionOverwrites
            val roleOverrides: Collection<RolePermissionOverwrite>? =
                server?.channels?.find(channel!!.id)?.rolePermissionOverwrites

            val allow = PermissionSet(0)
            val deny = PermissionSet(3072) // read + send
            val everyoneRole = server?.roles?.filter { r -> r.isEveryone }!!
            val oldName = channel?.name

            if (userOverrides == null || roleOverrides == null) {
                message.channel.send("No user/role overrides found!")
            } else {
                channel?.edit {
                    this.name = "archived-$archivedChannelsNum"
                    this.userPermissionOverwrites.removeAll(userOverrides)
                    this.rolePermissionOverwrites.removeAll(roleOverrides)
                    this.rolePermissionOverwrites.add(RolePermissionOverwrite(everyoneRole[0], allow, deny))
                }
                message.channel.send {
                    embed {
                        author(name = message.author?.name)
                        field("Success", "Changed name from `$oldName` to `archived-$archivedChannelsNum`", true)
                        color = Main.Colors.SUCCESS.color
                    }
                }
            }

        }
    }
}
