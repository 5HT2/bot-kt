import net.ayataka.kordis.entity.channel.TextChannel
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object StringHelper {
    fun String.isUrl(): Boolean {
        return Regex("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)").matches(
            this
        )
    }

    fun String.normalizeCase(): String {
        return this.substring(0, 1) + this.substring(1).toLowerCase()
    }

    fun String.trim(last: Int): String {
        return this.substring(0, this.length - last)
    }

    fun String.downloadBytes(): ByteArray {
        return URL(this).readBytes()
    }

    fun String.writeBytes(url: String): Int {
        val bytes = url.downloadBytes()
        File(this).writeBytes(bytes)
        return bytes.size
    }

    suspend fun String.runCommand(channel: TextChannel): Boolean {
        return try {
            this.runCommand()
            true
        } catch (e: IOException) {
            channel.send {
                embed {
                    title = "Error"
                    description = "```" + e.message + "```\n```" + e.stackTrace.joinToString("\n") + "```"
                    color = Colors.error
                }
            }
            false
        }
    }

    private fun String.runCommand() {
        this.runCommand(File(Paths.get(System.getProperty("user.dir")).toString()))
    }

    private fun String.runCommand(workingDir: File) {
        ProcessBuilder(*split(" ").toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(10, TimeUnit.MINUTES)
    }
}
