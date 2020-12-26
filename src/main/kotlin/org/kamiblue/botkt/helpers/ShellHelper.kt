package org.kamiblue.botkt.helpers

import org.kamiblue.botkt.helpers.ShellHelper.bash
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.nio.file.Paths

fun main() {
    println("ls # /proc/cpuinfo # | grep \"model name\" | cut -c 14-".bash())
}

object ShellHelper {
    val workingDir = File(Paths.get(System.getProperty("user.dir")).toString())

    /**
     * Run a command and send the output to stdout / stderr
     * @throws IOException
     */
    fun String.systemBash() {
        this.process().systemProcess().start()
    }

    /**
     * Run a command and send the output to stdout / stderr
     * @throws IOException
     */
    fun String.systemBash(dir: String) {
        this.process(dir).systemProcess().start()
    }

    /**
     * Run a command but return the output
     * // TODO: stderr does not return
     * @return shell stdout and stderr as a String
     * @throws IOException
     */
    fun String.bash(): String {
        return BufferedReader(InputStreamReader(this.process().start().inputStream)).nonNullLines()
    }

    /**
     * Run a command but return the output
     * // TODO: stderr does not return
     * @return shell stdout and stderr as a String
     * @throws IOException
     */
    fun String.bash(dir: String): String {
        return BufferedReader(InputStreamReader(this.process(dir).start().inputStream)).nonNullLines()
    }

    /**
     * @return a process with the working directory set to the path the program is running from
     */
    private fun String.process(): ProcessBuilder {
        return ProcessBuilder(listOf("/bin/bash", "-c", this)).directory(workingDir)
    }

    /**
     * @return a process with the working directory set to [dir]
     */
    private fun String.process(dir: String): ProcessBuilder {
        return ProcessBuilder(listOf("/bin/bash", "-c", this)).directory(File(dir))
    }

    /**
     * @return a process that outputs to stdout and stderr
     */
    private fun ProcessBuilder.systemProcess(): ProcessBuilder {
        return this.redirectOutput(INHERIT).redirectError(INHERIT)
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
