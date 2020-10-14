import PermissionTypes.COUNCIL_MEMBER
import Send.error
import net.ayataka.kordis.entity.message.Message

object Permissions {
    suspend fun Message.hasPermission(permission: PermissionTypes): Boolean {
        this.author?.let {
            return if (!it.id.hasPermission(permission)) {
                this.error("You don't have permission to use this command!")
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
    MANAGE_CONFIG,
    UPDATE_COUNTERS,
    ANNOUNCE
}