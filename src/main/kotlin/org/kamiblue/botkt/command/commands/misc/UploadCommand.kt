package org.kamiblue.botkt.command.commands.misc

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.error
import org.kamiblue.botkt.utils.upload
import java.io.File

object UploadCommand : BotCommand(
    name = "upload",
    category = Category.MISC
) {
    init {
        literal("tts") {
            execute {
                channel.send {
                    content = "TTS test"
                    tts = true
                }
            }
        }

        literal("all") {
            execute {
                val file = File(System.getProperty("user.dir"))
                val list = file.listFiles()?.filter { it.isFile } ?: emptyList()
                println(list)
                channel.upload(list, "Uploading all shits!")
                channel.send {


                }
            }
        }

        string("file name") {
            execute {
                val name = it.value
                val file = File(name)
                if (!file.exists()) {
                    channel.error("No file found for $name")
                } else {
                    channel.upload(file, "Here you go!")
                }
            }
        }
    }
}