package commands

import Colors
import Command
import doesLater

object ServerInfoCommand : Command("serverinfo") {
    init {
        doesLater {
            message.channel.send {
                embed {
                    color = Colors.primary
                    server?.name?.let { title = it }
                    server?.icon?.url?.let { thumbnailUrl = it }
                    server?.owner?.id?.let { field("Owner ID:", it) }
                    server?.region?.name?.let { field("Region:", it) }
                    server?.verificationLevel?.name?.let { field("Verification Level:", it, true) }
                    server?.voiceChannels?.size?.let { field("Voice Channels:", it, true) }
                    server?.textChannels?.size?.let { field("Text Channels:", it, true) }
                    server?.members?.size?.let { field("Members:", it, true) }
                    server?.roles?.size?.let { field("Roles:", it, true) }
                }
            }
        }
    }
}