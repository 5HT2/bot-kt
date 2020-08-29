package commands

import Command
import Main
import doesLater
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

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
                "pm2 reload bot-kt".runCommand(File(Paths.get(System.getProperty("user.dir")).toString()))
                message.channel.send {
                    embed {
                        title = "Rebooting..."
                        color = Main.Colors.SUCCESS.color
                    }
                }
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

    private fun String.runCommand(workingDir: File) {
        ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(1, TimeUnit.MINUTES)
    }
}