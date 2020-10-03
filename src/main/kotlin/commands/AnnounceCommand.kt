package commands

import Colors
import Command
import ConfigManager.readConfigSafe
import ConfigType
import PermissionTypes
import Permissions.hasPermission
import Send.error
import UserConfig
import arg
import doesLater
import greedyString

object AnnounceCommand : Command("announce") {
    init {
        greedyString("content") {
            doesLater { context ->
                val content: String = context arg "content"
                if (!message.hasPermission(PermissionTypes.ANNOUNCE)) {
                    return@doesLater
                }

                val channel = readConfigSafe<UserConfig>(ConfigType.USER, false)?.announceChannel ?: run {
                    message.error("Please configure the `announcementChannel` in the `${ConfigType.USER.configPath.substring(7)}` config!")
                    return@doesLater
                }

                server?.textChannels?.find(channel)?.send {
                    embed {
                        description = content
                        color = Colors.primary
                    }
                } ?: run { message.error("Error sending message! Channel `$channel` couldn't be found") }
            }
        }
    }
}