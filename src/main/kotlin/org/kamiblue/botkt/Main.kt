package org.kamiblue.botkt

import kotlinx.coroutines.*
import net.ayataka.kordis.DiscordClient
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.command.commands.CapeCommand
import org.kamiblue.botkt.command.commands.CounterCommand
import kotlin.system.exitProcess

object Main {
    private lateinit var processes: Array<Job>

    lateinit var client: DiscordClient
    var ready = false

    var prefix: Char? = null
        private set
        get() {
            return field ?: run {
                (readConfigSafe<UserConfig>(ConfigType.USER, false)?.prefix ?: ';').also {
                    field = it
                }
            }
        }

    const val currentVersion = "v1.4.0"

    @JvmStatic
    fun main(vararg args: String) = runBlocking {
        processes = arrayOf(
            launch {
                Bot.start()
            },

            runLooping(50) {
                CommandManager.runQueued()
            },

            runLooping {
                val loopDelay = readConfigSafe<CounterConfig>(ConfigType.COUNTER, false)?.updateInterval ?: 600000L
                delay(loopDelay)
                CounterCommand.updateChannel()
            },

            runLooping(30000) {
                try {
                    CapeCommand.save()
                    delay(30000)
                    CapeCommand.commit()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    private fun CoroutineScope.runLooping(loopDelay: Long = 50L, block: suspend CoroutineScope.() -> Unit) = launch {
        launch {
            while (isActive) {
                delay(loopDelay)
                block.invoke(this)
            }
        }
    }

    fun exit() {
        processes.forEach { it.cancel() }
        exitProcess(0)
    }
}
