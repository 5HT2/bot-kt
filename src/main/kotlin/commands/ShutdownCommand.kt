package commands

import Command
import Main
import PermissionTypes
import Permissions
import StringHelper
import StringHelper.MessageTypes.MISSING_PERMISSIONS
import doesLater
import kotlin.system.exitProcess

object ShutdownCommand : Command("shutdown") {
    init {
        doesLater {
            if (!Permissions.hasPermission(message, PermissionTypes.REBOOT_BOT)) {
                StringHelper.sendMessage(message.channel, MISSING_PERMISSIONS)
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
