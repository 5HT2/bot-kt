package org.kamiblue.botkt

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import net.ayataka.kordis.DiscordClient
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.server.enums.UserStatus
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.config.GlobalConfig
import org.kamiblue.botkt.config.GlobalConfigs
import org.kamiblue.botkt.config.ServerConfig
import org.kamiblue.botkt.config.ServerConfigs
import org.kamiblue.botkt.config.global.SystemConfig
import org.kamiblue.botkt.event.BotEventBus
import org.kamiblue.botkt.event.KordisEventProcessor
import org.kamiblue.botkt.event.events.ShutdownEvent
import org.kamiblue.botkt.manager.ManagerLoader
import org.kamiblue.botkt.plugin.PluginManager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.UpdateHelper
import org.kamiblue.commons.utils.ClassUtils
import java.io.PrintStream
import java.time.Instant
import kotlin.system.exitProcess

object Main {

    const val currentVersion = "v1.8.9"

    val startUpTime: Instant = Instant.now()
    @Suppress("EXPERIMENTAL_API_USAGE")
    val mainScope = CoroutineScope(newSingleThreadContext("Bot-kt Main"))
    val logger: Logger = LogManager.getLogger("Bot-kt")

    lateinit var client: DiscordClient; private set
    lateinit var discordHttp: HttpClient; private set
    lateinit var httpClient: HttpClient; private set

    var ready = false; private set

    internal fun exit() {
        exitProcess(0)
    }

    @JvmStatic
    fun main(vararg args: String) {
        System.setOut(createLoggingProxy("STDOUT", System.out, Level.INFO))
        System.setErr(createLoggingProxy("STDERR", System.err, Level.ERROR))

        addShutdownHook()
        start()
        BackgroundScope.start()
        Console.start()
    }

    private fun createLoggingProxy(name: String, realPrintStream: PrintStream, level: Level): PrintStream {
        return object : PrintStream(realPrintStream) {
            private val logger = LogManager.getLogger(name)

            override fun print(string: String?) {
                logger.log(level, string)
            }

            override fun println(string: String?) {
                logger.log(level, string)
            }

            override fun print(x: Any?) {
                logger.log(level, x)
            }

            override fun println(x: Any?) {
                logger.log(level, x)
            }
        }
    }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            Thread(
                {
                    logger.info("Bot shutting down, posting ShutdownEvent")
                    BotEventBus.post(ShutdownEvent)
                },
                "Bot Shutdown Hook"
            )
        )
    }

    private fun start() {
        runBlocking {
            val started = System.currentTimeMillis()

            logger.info("Starting bot!")
            registerConfigs()

            val deferred = mainScope.async { PluginManager.getLoaders() }
            login()

            UpdateHelper.writeVersion(currentVersion)
            UpdateHelper.updateCheck()

            updateStatus()

            CommandManager.init()
            ManagerLoader.load()
            PluginManager.loadAll(deferred.await())
            initHttpClients()

            client.addListener(KordisEventProcessor)
            ready = true

            val initMessage = "Initialized bot!\n" +
                "Running on $currentVersion\n" +
                "Startup took ${System.currentTimeMillis() - started}ms"

            initMessage.lines().forEach { logger.info(it) }
            mainScope.launch {
                sendStartupMessageToServers(initMessage)
            }
        }
    }

    private fun registerConfigs() {
        ClassUtils.findClasses("org.kamiblue.botkt.config.global", GlobalConfig::class.java).forEach {
            GlobalConfigs.register(ClassUtils.getInstance(it))
        }

        ClassUtils.findClasses("org.kamiblue.botkt.config.server", ServerConfig::class.java).forEach {
            ServerConfigs.register(it)
        }
    }

    private fun initHttpClients() {
        discordHttp = HttpClient {
            install(JsonFeature) {
                serializer = defaultSerializer()
            }
            defaultRequest {
                header("Authorization", "Bot ${SystemConfig.botToken}")
            }
        }

        httpClient = HttpClient {
            install(JsonFeature) {
                serializer = GsonSerializer() {
                    setLenient()
                }
            }
        }
    }

    private suspend fun login() {
        if (SystemConfig.botToken.isEmpty()) {
            logger.error("Bot token is empty, exiting. Make sure your file is formatted correctly!")
            exit()
            return
        }

        client = Kordis.create {
            token = SystemConfig.botToken
        }
    }

    private fun updateStatus() {
        client.updateStatus(UserStatus.ONLINE, SystemConfig.statusType, SystemConfig.statusMessage)
    }

    private suspend fun sendStartupMessageToServers(initMessage: String) {
        val startUpChannel = SystemConfig.startUpChannel
        if (startUpChannel.isEmpty()) return

        if (SystemConfig.startupServer == -1L) {
            client.servers.forEach {
                delay(100) // we don't want to hit the message rate limit, 10 messages a second should be fine
                it.textChannels.findByName(startUpChannel)?.sendStartUpMessage(initMessage)
            }
        } else {
            val channel = client.servers.find(SystemConfig.startupServer)?.textChannels?.findByName(startUpChannel)
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
