package org.kamiblue.botkt

import kotlinx.coroutines.*
import net.ayataka.kordis.DiscordClient
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.command.commands.CapeCommand
import org.kamiblue.botkt.command.commands.CounterCommand
import org.kamiblue.botkt.utils.configUpdateInterval
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

            launch {
                while (isActive) {
                    delay(configUpdateInterval())
                    CounterCommand.updateChannel()
                }
            },

            launch {
                while (isActive) {
                    delay(60000) // 1 minute
                    CapeCommand.save()
                }
            },

            launch {
                while (isActive) {
                    delay(60010) // 1 minute
                    CapeCommand.commit()
                }
            }
        )
    }

    fun exit() {
        processes.forEach { it.cancel() }
        exitProcess(0)
    }
}