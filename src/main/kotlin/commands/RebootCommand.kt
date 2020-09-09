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
                "java -jar bot-kt-${Main.currentVersion}.jar".runCommand(File(Paths.get(System.getProperty("user.dir")).toString()))
                // TODO: KILL ORIGINAL BOT
            } catch (e: IOException) {
                message.channel.send {
                    embed {
                        title = "Error"
                        description = e.message + "\n" + e.stackTrace.joinToString("\n")
                        color = Main.Colors.ERROR.color
                    }
                }
            }
        }
    }
}