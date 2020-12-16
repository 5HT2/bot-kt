package org.kamiblue.botkt

import kotlinx.coroutines.*
import net.ayataka.kordis.DiscordClient
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.server.enums.ActivityType
import net.ayataka.kordis.entity.server.enums.UserStatus
import net.ayataka.kordis.utils.timer
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

    const val currentVersion = "v1.5.0"

    @Suppress("EXPERIMENTAL_API_USAGE")
    val mainScope = CoroutineScope(newSingleThreadContext("Bot-kt Main"))
    val logger: Logger = LogManager.getLogger("Bot-kt")

    lateinit var client: DiscordClient; private set
    var ready = false; private set
    var prefix: Char? = null
        private set
        get() {
            return field ?: run {
                (ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)?.prefix ?: ';').also {
                    field = it
                }
            }
        }

    @JvmStatic
    fun main(vararg args: String) {
        addShutdownHook()

        runBlocking {
            start()

            BackgroundScope.launch(600000, "Failed to updated counter channels") {
                CounterCommand.updateChannel()
                logger.debug("Updated counter channels")
            }

            BackgroundScope.launch(600000, "Failed to save/commit capes") {
                CapeCommand.save()
                logger.debug("Saved capes")
                delay(300000)
                CapeCommand.commit()
                logger.debug("Commit capes")
            }

            mainScope.timer(10) {
                CommandManager.runQueued()
            }
        }
    }

    fun exit() {
        exitProcess(0)
    }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread({
            logger.info("Bot shutting down, posting ShutdownEvent")
            BotEventBus.post(ShutdownEvent)
        }, "Bot Shutdown Hook"))
    }

    private suspend fun start() {
        val started = System.currentTimeMillis()

        logger.info("Starting bot!")
        login()

        UpdateHelper.writeVersion(currentVersion)
        UpdateHelper.updateCheck()

        val userConfig = ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)
        updateStatus(userConfig)

        CommandManager.init()
        ManagerLoader.load()

        client.addListener(KordisEventProcessor)
        ready = true

        val initMessage = "Initialized bot!\n" +
            "Running on $currentVersion\n" +
            "Startup took ${System.currentTimeMillis() - started}ms"

        initMessage.lines().forEach { logger.info(it) }
        mainScope.launch {
            sendStartupMessageToServers(userConfig, initMessage)
        }
    }

    private suspend fun login() {
        val config = ConfigManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            logger.error("Bot token not found, exiting. Make sure your file is formatted correctly!")
            exit()
            return
        }

        client = Kordis.create {
            token = config.botToken
        }
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

}
