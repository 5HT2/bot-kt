package org.kamiblue.botkt

import kotlinx.coroutines.delay
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.entity.server.enums.ActivityType
import net.ayataka.kordis.entity.server.enums.UserStatus
import org.kamiblue.botkt.command.CommandManager
import org.kamiblue.botkt.event.KordisEventProcessor
import org.kamiblue.botkt.helpers.UpdateHelper
import org.kamiblue.botkt.manager.ManagerLoader
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.Colors

/**
 * @author l1ving
 * @since 16/08/20 17:30
 */
object Bot {

    suspend fun start() {
        val started = System.currentTimeMillis()

        Main.logger.info("Starting bot!")

        UpdateHelper.writeVersion(Main.currentVersion)
        UpdateHelper.updateCheck()

        val config = ConfigManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            Main.logger.error("Bot token not found, exiting. Make sure your file is formatted correctly!")
            Main.exit()
            return
        }

        Main.client = Kordis.create {
            token = config.botToken
        }

        CommandManager.init()
        ManagerLoader.load()

        val userConfig = ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)

        updateStatus(userConfig)

        delay(2000) // Discord API is really stupid and doesn't give you the information you need right away, hence delay needed


        val initMessage = listOf(
            "Initialized bot!",
            "Running on ${Main.currentVersion}",
            "Startup took ${System.currentTimeMillis() - started}ms"
        )
        sendStartupMessage(userConfig, initMessage.joinToString("\n"))
        initMessage.forEach { Main.logger.info(it) }

        Main.client.addListener(KordisEventProcessor)
        Main.ready = true
    }

    private fun updateStatus(userConfig: UserConfig?) {
        userConfig?.statusMessage?.let { message ->
            val type = userConfig.statusMessageType?.let {
                ActivityType.values().getOrNull(it)
            } ?: ActivityType.UNKNOWN

            Main.client.updateStatus(UserStatus.ONLINE, type, message)
        }
    }

    private suspend fun sendStartupMessage(userConfig: UserConfig?, initMessage: String) {
        userConfig?.startUpChannel?.let {
            if (userConfig.primaryServerId == null) {
                Main.client.servers.forEach { server ->
                    delay(100) // we don't want to hit the message rate limit, 10 messages a second should be fine
                    server.textChannels.findByName(it)?.send {
                        embed {
                            title = "Startup"
                            description = initMessage
                            color = Colors.SUCCESS.color
                        }
                    }
                }
            } else {
                val channel = Main.client.servers.find(userConfig.primaryServerId)?.textChannels?.findByName(it)
                channel?.send {
                    embed {
                        title = "Startup"
                        description = initMessage
                        color = Colors.SUCCESS.color
                    }
                }
            }
        }
    }
}

