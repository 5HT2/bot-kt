package org.kamiblue.botkt.command.options

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.command.execute.ExecuteOption
import org.kamiblue.commons.utils.MathUtils

class Timeout(private val timeoutMillis: Long, private val resetOnExecute: Boolean) :
    ExecuteOption<MessageExecuteEvent> {
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
