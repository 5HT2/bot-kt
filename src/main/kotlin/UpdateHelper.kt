import Main.currentVersion
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * @author dominikaaaa
 * @since 2020/08/25 20:06
 */
object UpdateHelper {
    fun updateCheck() {
        if (File("noUpdateCheck").exists()) return
        val versionConfig = ConfigManager.readConfigFromUrl<VersionConfig>("https://raw.githubusercontent.com/kami-blue/bot-kt/master/version.json")

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
        val userConfig = ConfigManager.readConfig<UserConfig>(ConfigType.USER, false)

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

        val file = Paths.get("$path/currentVersion")
        File(file.toString()).delete()
        Files.newBufferedWriter(file).use {
            it.write(version)
        }

        println("Auto Update - Finished updating to $version")

        userConfig.autoUpdateRestart?.let {
            if (it) {
                Main.process?.let { process ->
                    println("Auto Update - Restarting bot")
                    process.cancel()
                    exitProcess(0)
                }
            }
        }
    }
}
