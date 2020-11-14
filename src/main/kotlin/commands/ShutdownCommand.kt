package org.kamiblue.botkt.commands

import org.kamiblue.botkt.Colors
import org.kamiblue.botkt.Command
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.PermissionTypes.REBOOT_BOT
import org.kamiblue.botkt.doesLaterIfHas

object ShutdownCommand : Command("shutdown") {
    init {
        doesLaterIfHas(REBOOT_BOT) {
            message.channel.send {
                embed {
                    title = "Shutting down..."
                    color = Colors.success
                }
            }
            Main.exit()
        }
    }
}
