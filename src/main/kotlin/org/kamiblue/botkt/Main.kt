package org.kamiblue.botkt

import kotlinx.coroutines.*
import net.ayataka.kordis.DiscordClient
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.command.commands.github.CounterCommand
import org.kamiblue.botkt.command.commands.misc.CapeCommand
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

    const val currentVersion = "v1.4.6"

    @JvmStatic
    fun main(vararg args: String) = runBlocking {
        processes = arrayOf(
            launch {
                Bot.start()
            },

            runLooping(50) {
                CommandManager.runQueued()
            },

            runLooping(600000) {
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
        while (isActive) {
            delay(loopDelay)
            try {
                block.invoke(this)
            } catch (e: Exception) {
                // this is fine, these are running in the background
            }
        }
    }

    fun exit() {
        processes.forEach { it.cancel() }
        exitProcess(0)
    }
}
