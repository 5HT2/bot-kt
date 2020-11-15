package commands

import Colors
import Command
import Main
import PermissionTypes.REBOOT_BOT
import doesLaterIfHas

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
