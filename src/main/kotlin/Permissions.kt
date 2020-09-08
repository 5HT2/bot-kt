import PermissionTypes.COUNCIL_MEMBER
import net.ayataka.kordis.entity.message.Message

object Permissions {
    fun hasPermission(message: Message, permission: PermissionTypes): Boolean {
        return hasPermission(message.author!!.id, false, permission)
    }

    fun hasPermission(id: Long, permission: PermissionTypes): Boolean {
        return hasPermission(id, false, permission)
    }

    fun hasPermission(id: Long, reload: Boolean, permission: PermissionTypes): Boolean {
        var has = false
        FileManager.readConfigSafe<PermissionConfig>(ConfigType.PERMISSION, reload)?.let {
            it.councilMembers[id]?.forEach { peit ->
                if (peit == permission) has = true
            }
        }
        return has
    }
}

/**
 * [COUNCIL_MEMBER] affects:
 *   - DiscussCommand
 */
enum class PermissionTypes {
    ARCHIVE_CHANNEL,
    COUNCIL_MEMBER,
    REBOOT_BOT
}