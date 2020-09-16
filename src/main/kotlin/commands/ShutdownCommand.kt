package commands

import Command
import Main
import PermissionTypes
import Permissions.hasPermission
import doesLater
import kotlin.system.exitProcess

object ShutdownCommand : Command("shutdown") {
    init {
        doesLater {
            if (!message.hasPermission(PermissionTypes.REBOOT_BOT)) {
                return@doesLater
            }

            message.channel.send {
                embed {
                    title = "Shutting down..."
                    color = Main.Colors.SUCCESS.color
                }
            }
            Main.process?.cancel()
            exitProcess(0)
        }
    }
}
