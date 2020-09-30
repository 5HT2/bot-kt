import Send.stackTrace
import net.ayataka.kordis.entity.message.Message
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Paths

object ShellHelper {
    val workingDir = File(Paths.get(System.getProperty("user.dir")).toString())

    /**
     * Run a command and send the stderr to the channel
     * @return false if command errored
     */
    suspend fun List<String>.runCommand(message: Message): Boolean {
        return try {
            this.runCommand()
            true
        } catch (e: IOException) {
            message.stackTrace(e)
            false
        }
    }

    /**
     * Run a command and send the stderr to the channel
     * @return false if command errored
     */
    suspend fun String.runCommand(message: Message): Boolean {
        return try {
            this.runCommand()
            true
        } catch (e: IOException) {
            message.stackTrace(e)
            false
        }
    }

    /**
     * Run a command with the output being sent to stdout and stderr
     */
    fun List<String>.runCommand() {
        this.process().systemProcess().start()
    }

    /**
     * Run a command with the output being sent to stdout and stderr
     */
    fun String.runCommand() {
        this.process().systemProcess().start()
    }

    /**
     * Run a command but return the output
     * @return shell stdout and stderr as a String
     */
    fun List<String>.runCommandOutput(): String {
        return BufferedReader(InputStreamReader(this.process().start().inputStream)).nonNullLines()
    }

    /**
     * Run a command but return the output
     * @return shell stdout and stderr as a String
     */
    fun String.runCommandOutput(): String {
        return BufferedReader(InputStreamReader(this.process().start().inputStream)).nonNullLines()
    }

    /**
     * Example for a piped command:
     * `ls -l | grep foo` # this turns into # `"/bin/sh", "-c", "\"ls -l| grep foo\""`
     * @return a process with the working directory set to the path the program is running from
     */
    private fun List<String>.process(): ProcessBuilder {
        return ProcessBuilder(this).directory(workingDir)
    }

    /**
     * Splits the single string by spaces into a command
     * @return a process with the working directory set to the path the program is running from
     */
    private fun String.process(): ProcessBuilder {
        return ProcessBuilder(*split(" ").toTypedArray()).directory(workingDir)
    }

    /**
     * @return a process that outputs to stdout and stderr
     */
    private fun ProcessBuilder.systemProcess(): ProcessBuilder {
        return this.redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT)
    }

    /**
     * Process a [BufferedReader]'s non-null lines as one String
     * @return all lines of a [BufferedReader] as one String
     */
    private fun BufferedReader.nonNullLines(): String {
        val builder = StringBuilder()
        var line: String?

        while (this.readLine().also { line = it } != null) {
            builder.append(line)
            builder.append(System.getProperty("line.separator"))
        }
        return builder.toString()
    }
}