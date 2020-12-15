package org.kamiblue.botkt

import kotlinx.coroutines.*
import net.ayataka.kordis.DiscordClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.command.commands.github.CounterCommand
import org.kamiblue.botkt.command.commands.misc.CapeCommand
import org.kamiblue.botkt.manager.managers.ConfigManager.readConfigSafe
import org.kamiblue.botkt.manager.managers.MuteManager
import kotlin.system.exitProcess

object Main {

    val logger: Logger = LogManager.getLogger("bot-kt")

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

    private lateinit var processes: Array<Job>

    const val currentVersion = "v1.5.0"

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
                logger.debug("Updated counter channels")
            },

            runLooping(30000) {
                try {
                    CapeCommand.save()
                    delay(30000)
                    CapeCommand.commit()
                } catch (e: Exception) {
                    logger.warn("Failed to save/commit capes", e)
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
        try {
            CapeCommand.save()
            MuteManager.save()
        } catch (e: Exception) {
            // this is fine
        }
        exitProcess(0)
    }
}
