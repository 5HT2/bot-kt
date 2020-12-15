package org.kamiblue.botkt

import kotlinx.coroutines.*
import net.ayataka.kordis.DiscordClient
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.server.enums.ActivityType
import net.ayataka.kordis.entity.server.enums.UserStatus
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.command.commands.github.CounterCommand
import org.kamiblue.botkt.command.commands.misc.CapeCommand
import org.kamiblue.botkt.event.BotEventBus
import org.kamiblue.botkt.event.KordisEventProcessor
import org.kamiblue.botkt.event.events.ShutdownEvent
import org.kamiblue.botkt.helpers.UpdateHelper
import org.kamiblue.botkt.manager.ManagerLoader
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.Colors
import kotlin.system.exitProcess

object Main {

    val logger: Logger = LogManager.getLogger("bot-kt")

    lateinit var client: DiscordClient
    var ready = false
    var prefix: Char? = null
        private set
        get() {
            return field ?: run {
                (ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)?.prefix ?: ';').also {
                    field = it
                }
            }
        }

    private lateinit var processes: Array<Job>

    const val currentVersion = "v1.5.0"

    @JvmStatic
    fun main(vararg args: String) {
        addShutdownHook()

        runBlocking {
            processes = arrayOf(
                launch {
                    start()
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
    }

    fun exit() {
        processes.forEach { it.cancel() }
        exitProcess(0)
    }

    private fun addShutdownHook(){
        Runtime.getRuntime().addShutdownHook(Thread({
            logger.info("Bot shutting down, posting ShutdownEvent")
            BotEventBus.post(ShutdownEvent)
        }, "Bot Shutdown Hook"))
    }


    private suspend fun start() {
        val started = System.currentTimeMillis()

        logger.info("Starting bot!")

        UpdateHelper.writeVersion(currentVersion)
        UpdateHelper.updateCheck()

        val config = ConfigManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            logger.error("Bot token not found, exiting. Make sure your file is formatted correctly!")
            exit()
            return
        }

        client = Kordis.create {
            token = config.botToken
        }

        CommandManager.init()
        ManagerLoader.load()

        val userConfig = ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)

        updateStatus(userConfig)

        delay(2000) // Discord API is really stupid and doesn't give you the information you need right away, hence delay needed

        client.addListener(KordisEventProcessor)
        ready = true

        val initMessage = "Initialized bot!\n" +
            "Running on $currentVersion\n" +
            "Startup took ${System.currentTimeMillis() - started - 2000}ms"

        initMessage.lines().forEach { logger.info(it) }
        sendStartupMessageToServers(userConfig, initMessage)
    }

    private fun updateStatus(userConfig: UserConfig?) {
        val message = userConfig?.statusMessage ?: return
        val typeIndex = userConfig.statusMessageType ?: return
        val type = ActivityType.values().getOrNull(typeIndex) ?: return
        client.updateStatus(UserStatus.ONLINE, type, message)
    }

    private suspend fun sendStartupMessageToServers(userConfig: UserConfig?, initMessage: String) {
        val startUpChannel = userConfig?.startUpChannel ?: return

        if (userConfig.primaryServerId == null) {
            client.servers.forEach {
                delay(100) // we don't want to hit the message rate limit, 10 messages a second should be fine
                it.textChannels.findByName(startUpChannel)?.sendStartUpMessage(initMessage)
            }
        } else {
            val channel = client.servers.find(userConfig.primaryServerId)?.textChannels?.findByName(startUpChannel)
            channel?.sendStartUpMessage(initMessage)
        }
    }

    private suspend fun TextChannel.sendStartUpMessage(initMessage: String) {
        send {
            embed {
                title = "Startup"
                description = initMessage
                color = Colors.SUCCESS.color
            }
        }
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
}
