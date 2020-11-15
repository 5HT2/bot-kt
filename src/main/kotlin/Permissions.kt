package org.kamiblue.botkt

import org.kamiblue.botkt.helpers.StringHelper.toHumanReadable
import net.ayataka.kordis.entity.message.Message
import org.kamiblue.botkt.Send.error

object Permissions {
    suspend fun Message.hasPermission(permission: PermissionTypes): Boolean {
        this.author?.let {
            return if (!it.id.hasPermission(permission)) {
                this.missingPermissions(permission)
                false
            } else {
                true
            }
        } ?: run {
            this.error("Message `${this.id}` author was null")
            return false
        }
    }

    fun Long.hasPermission(permission: PermissionTypes): Boolean {
        var has = false
        ConfigManager.readConfigSafe<PermissionConfig>(ConfigType.PERMISSION, false)?.let {
            it.councilMembers[this]?.forEach { peit ->
                if (peit == permission) has = true
            }
        }
        return has
    }

    suspend fun Message.missingPermissions(permission: PermissionTypes) {
        this.channel.send {
            embed {
                title = "Missing permission"
                description = "Sorry, but you're missing the '${permission.name.toHumanReadable()}' permission, which is required to run this command."
                color = Colors.error
            }
        }
    }
}

@Suppress("UNUSED")
enum class PermissionTypes {
    ARCHIVE_CHANNEL,
    COUNCIL_MEMBER,
    REBOOT_BOT,
    MANAGE_CONFIG,
    UPDATE_COUNTERS,
    ANNOUNCE,
    MANAGE_CHANNELS,
    MASS_BAN,
    APPROVE_ISSUE_CREATION
}