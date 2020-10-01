package commands

import Command
import ConfigManager.readConfigSafe
import Permissions.hasPermission
import Send.error
import UserConfig
import greedyString
import arg
import doesLater

object AnnounceCommand : Command("announce") {
    init {
        greedyString("content"){
            doesLater{context ->
                val content: String? = context arg "content"
                if (content.isNullOrEmpty()) {
                    message.error("You need to put announcement here.")
                    return@doesLater
                } else if (!message.hasPermission(PermissionTypes.COUNCIL_MEMBER)){
                    message.error("You don't have the permission to do that!")
                    return@doesLater
                }
                message.server?.textChannels?.find(readConfigSafe<UserConfig>(ConfigType.USER, false)?.announceChannel ?: run {
                    message.error("Bad announcement channel found in config!")
                    return@doesLater
                })?.send(content)
            }
        }
    }
}