import net.ayataka.kordis.entity.channel.TextChannel
import java.io.File
import java.util.concurrent.TimeUnit

object StringHelper {
    fun String.isUrl(): Boolean {
        return Regex("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)").matches(
            this
        )
    }

    fun String.runCommand(workingDir: File) {
        ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(10, TimeUnit.MINUTES)
    }

    suspend fun sendMessage(channel: TextChannel, type: MessageTypes) {
        when (type) {
            MessageTypes.MISSING_PERMISSIONS -> {
                channel.send {
                    embed {
                        field("Error", "You don't have permission to use this command!", true)
                        color = Main.Colors.ERROR.color
                    }
                }
            }
        }
    }

    enum class MessageTypes {
        MISSING_PERMISSIONS
    }
}
