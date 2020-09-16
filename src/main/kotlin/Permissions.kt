import PermissionTypes.COUNCIL_MEMBER
import Send.error
import net.ayataka.kordis.entity.message.Message

object Permissions {
    suspend fun Message.hasPermission(permission: PermissionTypes): Boolean {
        this.author?.let {
            return if (!hasPermission(it.id, permission)) {
                this.error("You don't have permission to use this command!")
                false
            } else {
                true
            }
        } ?: run {
            this.error("Message (`${this.id}`) author was null")
            return false
        }
    }

    fun hasPermission(id: Long, permission: PermissionTypes): Boolean {
        return hasPermission(id, false, permission)
    }

    fun hasPermission(id: Long, reload: Boolean, permission: PermissionTypes): Boolean {
        var has = false
        ConfigManager.readConfigSafe<PermissionConfig>(ConfigType.PERMISSION, reload)?.let {
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
@Suppress("UNUSED")
enum class PermissionTypes {
    ARCHIVE_CHANNEL,
    COUNCIL_MEMBER,
    REBOOT_BOT,
    MANAGE_CONFIG
}