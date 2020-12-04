package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.utils.Colors

object ShutdownCommand : BotCommand(
    name = "shutdown"
) {
    init {
        executeIfHas(PermissionTypes.REBOOT_BOT) {
            message.channel.send {
                embed {
                    title = "Shutting down..."
                    color = Colors.SUCCESS.color
                }
            }
            Main.exit()
        }
    }
}
