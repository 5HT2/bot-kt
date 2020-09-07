package commands

import Command
import Main
import StringHelper.runCommand
import doesLater
import java.io.File
import java.io.IOException
import java.nio.file.Paths

object RebootCommand : Command("reboot") {
    init {
        doesLater {
            if (message.author?.id != 563138570953687061) {
                message.channel.send {
                    embed {
                        field("Error", "You don't have permission to use this command!", true)
                        color = Main.Colors.WARN.color
                    }
                }
                return@doesLater
            }

            try {
                message.channel.send {
                    embed {
                        title = "Rebooting..."
                        color = Main.Colors.SUCCESS.color
                    }
                }
                "pm2 reload bot-kt".runCommand(File(Paths.get(System.getProperty("user.dir")).toString()))
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