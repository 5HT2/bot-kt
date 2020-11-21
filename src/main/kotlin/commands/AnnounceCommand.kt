package org.kamiblue.botkt.commands

import org.kamiblue.botkt.*
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.Colors

object AnnounceCommand : Command("announce") {
    init {
        greedyString("content") {
            doesLaterIfHas(PermissionTypes.ANNOUNCE) { context ->
                val content: String = context arg "content"

                val channel = readConfigSafe<UserConfig>(ConfigType.USER, false)?.announceChannel ?: run {
                    message.error("Please configure the `announcementChannel` in the `${ConfigType.USER.configPath.substring(7)}` config!")
                    return@doesLaterIfHas
                }

                server?.textChannels?.find(channel)?.send {
                    embed {
                        description = content
                        color = Colors.PRIMARY.color
                    }
                } ?: run { message.error("Error sending message! Channel `$channel` couldn't be found") }
            }
        }
    }
}