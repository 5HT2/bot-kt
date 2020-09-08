package commands

import Command
import Main
import PermissionTypes
import Permissions
import StringHelper
import StringHelper.MessageTypes.MISSING_PERMISSIONS
import StringHelper.runCommand
import doesLater
import java.io.File
import java.io.IOException
import java.nio.file.Paths

object RebootCommand : Command("reboot") {
    init {
        doesLater {
            if (!Permissions.hasPermission(message, PermissionTypes.REBOOT_BOT)) {
                StringHelper.sendMessage(message.channel, MISSING_PERMISSIONS)
                return@doesLater
            }

            try {
                message.channel.send {
                    embed {
                        title = "Rebooting..."
                        color = Main.Colors.SUCCESS.color
                    }
                }
                "pm2 stop bot-kt && pm2 start bot-kt".runCommand(File(Paths.get(System.getProperty("user.dir")).toString()))
            } catch (e: IOException) {
                message.channel.send {
                    embed {
                        title = "Error"
                        description = "pm2 is not installed, failed to reboot bot."
                        color = Main.Colors.ERROR.color
                    }
                }
            }
        }
    }
}