package org.kamiblue.botkt

import net.ayataka.kordis.entity.message.Message
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageUtils.error
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable

object Permissions {
    suspend fun Message.hasPermission(permission: PermissionTypes): Boolean {
        return this.author?.let {
            return if (!it.id.hasPermission(permission)) {
                this.missingPermissions(permission)
                false
            } else {
                true
            }
        } ?: run {
            this.channel.error("Message `${this.id}` author was null")
            false
        }
    }

    fun Long.hasPermission(permission: PermissionTypes): Boolean {
        return ConfigManager.readConfigSafe<PermissionConfig>(ConfigType.PERMISSION, false)?.let {
            it.councilMembers[this]?.contains(permission)
        } ?: false
    }

    suspend fun Message.missingPermissions(permission: PermissionTypes) {
        this.channel.send {
            embed {
                title = "Missing permission"
                description = "Sorry, but you're missing the '${permission.name.toHumanReadable()}' permission, which is required to run this command."
                color = Colors.ERROR.color
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
    SAY,
    MANAGE_CHANNELS,
    MASS_BAN,
    APPROVE_ISSUE_CREATION,
    AUTHORIZE_CAPES,
    CREATE_RELEASE,
    PURGE_PROTECTED
}