import Main.currentVersion
import UpdateHelper.updateBot
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    updateBot("")
}

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
            println("Not up to date:\nCurrent version: $currentVersion\nLatest Version: ${versionConfig.version}\n")
        } else {
            println("Up to date! Running on $currentVersion")
        }
    }

    fun updateBot(version: String) {
        val userConfig = FileManager.readConfig<UserConfig>(ConfigType.USER, false)

        if (userConfig?.autoUpdate == null || !userConfig.autoUpdate) {
            return
        }

        val path = Paths.get(userConfig.installPath)
        if (Files.isDirectory(path)) {
            val deleted = arrayListOf<String>()
            File(userConfig.installPath).walk().forEach {
                if (it.name.matches(Regex("bot-kt.*.jar"))) {
                    deleted.add(it.name)
                    it.delete()
                }
            }
            println("Auto Update - Deleted the following files:\n" + deleted.joinToString { it })
        } else {
            println("Auto Update - Couldn't find install path \"$path\"")
        }
    }
}
