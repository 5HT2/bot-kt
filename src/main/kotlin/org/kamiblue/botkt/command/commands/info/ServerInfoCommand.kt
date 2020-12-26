package org.kamiblue.botkt.command.commands.info

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.Colors

object ServerInfoCommand : BotCommand(
    name = "serverinfo",
    alias = arrayOf("info", "guildinfo", "guild"),
    category = Category.INFO,
    description = "Get information about a server"
) {
    init {
        execute {
            message.channel.send {
                embed {
                    color = Colors.PRIMARY.color
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
