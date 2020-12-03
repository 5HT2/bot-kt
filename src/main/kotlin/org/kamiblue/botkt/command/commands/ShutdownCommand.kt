package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.PermissionTypes.REBOOT_BOT
import org.kamiblue.botkt.command.Command
import org.kamiblue.botkt.command.doesLaterIfHas
import org.kamiblue.botkt.utils.Colors

object ShutdownCommand : Command("shutdown") {
    init {
        doesLaterIfHas(REBOOT_BOT) {
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
