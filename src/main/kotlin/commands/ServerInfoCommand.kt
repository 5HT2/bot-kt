package commands

import Command
import Main
import doesLater

object ServerInfoCommand : Command("serverinfo") {
    init {
        doesLater {
            try {
                message.channel.send {
                    embed {
                        color = Main.Colors.BLUE.color
                        title = server!!.name
                        thumbnailUrl = server?.icon?.url
                        server?.owner?.id?.let { it -> field("Owner ID:", it) }
                        server?.region?.name?.let { it1 -> field("Region:", it1) }
                        server?.verificationLevel?.name?.let { it2 -> field("Verification Level:", it2, true) }
                        server?.voiceChannels?.size?.let { it3 -> field("Voice Channels:", it3, true) }
                        server?.textChannels?.size?.let { it4 -> field("Text Channels:", it4, true) }
                        server?.members?.size?.let { it5 -> field("Members:", it5, true) }
                        server?.roles?.size?.let { it7 -> field("Roles:", it7, true) }
                    }
                }
            } catch (e: Exception) {
                message.channel.send {
                    embed {
                        color = Main.Colors.ERROR.color
                        title = "Something went wrong when executing this command! Here's the stacktrace:"
                        description = "```$e```"
                    }
                }
            }
        }
    }
}