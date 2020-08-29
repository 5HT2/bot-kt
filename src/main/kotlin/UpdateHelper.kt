import Main.currentVersion
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * @author dominikaaaa
 * @since 2020/08/25 20:06
 */
object UpdateHelper {
    fun updateCheck() {
        if (File("noUpdateCheck").exists()) return
        val versionConfig = FileManager.readConfig<VersionConfig>(ConfigType.VERSION, false)

        if (versionConfig?.version == null) {
            println("Couldn't access remote version when checking for update")
            return
        }

        if (versionConfig.version != currentVersion) {
            println("Not up to date:\nCurrent version: $currentVersion\nLatest Version: ${versionConfig.version}")

            updateBot(versionConfig.version)
        } else {
            println("Up to date! Running on $currentVersion")
        }
    }

    private fun updateBot(version: String) {
        val userConfig = FileManager.readConfig<UserConfig>(ConfigType.USER, false)

        if (userConfig?.autoUpdate == null || !userConfig.autoUpdate) {
            return
        }

        val path = Paths.get(System.getProperty("user.dir"))
        val deleted = arrayListOf<String>()
        File(path.toString()).walk().forEach {
            if (it.name.matches(Regex("bot-kt.*.jar"))) {
                deleted.add(it.name)
                it.delete()
            }
        }

        if (deleted.isNotEmpty()) {
            println("Auto Update - Deleted the following files:\n" + deleted.joinToString { it })
        }

        val bytes =
            URL("https://github.com/kami-blue/bot-kt/releases/download/$version/bot-kt-$version.jar").readBytes()

        println("Auto Update - Downloaded bot-kt-$version.jar ${bytes.size / 1000000}MB")
        val appendSlash = if (path.endsWith("/")) "" else "/"
        val targetFile = path.toString() + appendSlash + "bot-kt-$version.jar"
        File(targetFile).writeBytes(bytes)
        println("Auto Update - Finished updating to $version")

        if (File("process.json").exists()) {

            println("Auto Update - Creating pm2 config")

            Pm2.createJson(version)

            println("Auto Update - Created pm2 config, reloading bot-kt")

            try {
                "pm2 reload bot-kt".runCommand(File(path.toString()))
            } catch (e: IOException) {
                println("pm2 is not installed, failed to reload bot.\nYou can ignore this if you're not using pm2, but you will need to manually restart the bot to start using the latest version.")
            }
        } else {
            println("Auto Update - You may now restart the bot to start using the latest version")
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
