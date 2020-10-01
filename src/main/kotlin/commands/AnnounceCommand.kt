package commands

import Command
import ConfigManager.readConfigSafe
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
                }
                Main.client?.servers?.find(message.server?.id ?: run {
                    message.error("Run this command inside a server!")
                    return@doesLater
                })?.textChannels?.find(readConfigSafe<UserConfig>(ConfigType.USER, false)?.announceChannel ?: run {
                    message.error("No annouce channel defined in config!")
                    return@doesLater
                })?.send(content)
            }
        }
    }
}