package commands

import Command
import ConfigManager.readConfigSafe
import Permissions.hasPermission
import Send.error
import Send.normal
import UserConfig
import greedyString
import arg
import doesLater

object AnnounceCommand : Command("announce") {
    init {
        greedyString("content"){
            doesLater{context ->
                val content: String = context arg "content"
                if (!message.hasPermission(PermissionTypes.ANNOUNCE)){
                    message.error("You don't have the permission to do that!")
                    return@doesLater
                }
                message.server?.textChannels?.find(readConfigSafe<UserConfig>(ConfigType.USER, false)?.announceChannel ?: run {
                    message.error("Please configure the `announcementChannel` in the `${ConfigType.USER.configPath.substring(7)}` config!")
                    return@doesLater
                })?.send {
                    embed {
                        description = content
                        color = Colors.primary
                    }
                }
            }
        }
    }
}