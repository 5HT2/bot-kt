package org.kamiblue.botkt

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.ayataka.kordis.DiscordClient
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.commands.CounterCommand
import org.kamiblue.botkt.utils.configUpdateInterval
import kotlin.system.exitProcess

object Main {
    private lateinit var process: Job
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

    const val currentVersion = "v1.2.5"

    @JvmStatic
    fun main(vararg args: String) = runBlocking {
        process = launch {
            Bot().start()
            while (true) {
                delay(configUpdateInterval())
                CounterCommand.updateChannel()
            }
        }
    }

    fun exit() {
        process.cancel()
        exitProcess(0)
    }
}
