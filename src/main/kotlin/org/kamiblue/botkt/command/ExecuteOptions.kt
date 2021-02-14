package org.kamiblue.botkt.command

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kamiblue.botkt.Console
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.botkt.utils.error
import org.kamiblue.command.execute.ExecuteOption
import org.kamiblue.commons.utils.MathUtils
import java.util.*
import kotlin.collections.HashMap

class HasPermission(private val permission: PermissionTypes) : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message is Console.FakeMessage
            || event.message.author.hasPermission(permission)
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.channel.send {
            embed {
                title = "Missing permission"
                description = "You need the permission `${permission.name.toHumanReadable()}` to use this command!"
                color = Colors.ERROR.color
            }
        }
    }
}

class AnyPermission(private vararg val permissions: PermissionTypes) : ExecuteOption<MessageExecuteEvent> {
    private val permissionListString = permissions.joinToString { "`${it.name.toHumanReadable()}`" }

    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message is Console.FakeMessage
            || permissions.any { event.message.author.hasPermission(it) }
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.channel.send {
            embed {
                title = "Missing permission"
                description = "You need one of the following permissions to use this command!\n$permissionListString"
                color = Colors.ERROR.color
            }
        }
    }
}

class HasRole(private val role: String) : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message.member?.roles?.any { it.name == role }
            ?: false
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        val role = event.server?.roles?.findByName(role, true) ?: return

        event.channel.send {
            embed {
                title = "Missing role"
                description = "You need the role `${role.name}` to use this command!"
                color = Colors.ERROR.color
            }
        }
    }
}

class AnyRole(vararg roles: String) : ExecuteOption<MessageExecuteEvent> {
    private val roles = roles.map { it.toLowerCase(Locale.ROOT) }.toHashSet()

    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.message.member?.roles?.any {
            roles.contains(it.name.toLowerCase(Locale.ROOT))
        } ?: false
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        val roles = event.server?.roles?.joinToString { "`${it.name}`" }

        event.channel.send {
            embed {
                title = "Missing role"
                description = "You need one of the following roles to use this command!\n$roles"
                color = Colors.ERROR.color
            }
        }
    }
}

class Timeout(private val timeoutMillis: Long, private val resetOnExecute: Boolean) : ExecuteOption<MessageExecuteEvent> {
    private val mutex = Mutex()
    private val timeoutMap = HashMap<Long, Long>()

    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        val id = event.message.author?.id ?: return false
        val currentTime = System.currentTimeMillis()

        val lastTime = mutex.withLock {
            cleanTimeoutMap()
            timeoutMap[id]
        }

        return if (lastTime == null || currentTime - lastTime > timeoutMillis) {
            if (resetOnExecute) reset(id, currentTime)
            true
        } else {
            false
        }
    }

    private fun cleanTimeoutMap() {
        val size = timeoutMap.size
        if (size > 500) {
            Main.mainScope.launch(Dispatchers.Default) {
                mutex.withLock {
                    Main.logger.debug("${this@Timeout} size is $size/500, cleaning up old values.")
                    timeoutMap.entries.removeIf { System.currentTimeMillis() - it.value > timeoutMillis }
                }
            }
        }
    }

    suspend fun reset(id: Long?, time: Long = System.currentTimeMillis()) {
        if (id == null) return

        mutex.withLock {
            timeoutMap[id] = time
        }
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        val id = event.message.author?.id ?: return

        val lastTime = mutex.withLock {
            timeoutMap[id] ?: return
        }

        val waitTime = MathUtils.round((timeoutMillis - (System.currentTimeMillis() - lastTime)) / 1000.0, 1)

        event.channel.send {
            embed {
                title = "Timeout"
                description = "You need to wait for $waitTime seconds before using this command again!"
                color = Colors.ERROR.color
            }
        }
    }
}

object DirectOnly : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.server == null
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.channel.error("This command can only be used in direct message chat!")
    }
}

object ServerOnly : ExecuteOption<MessageExecuteEvent> {
    override suspend fun canExecute(event: MessageExecuteEvent): Boolean {
        return event.server != null
    }

    override suspend fun onFailed(event: MessageExecuteEvent) {
        event.channel.error("This command can only be used in a server!")
    }
}
