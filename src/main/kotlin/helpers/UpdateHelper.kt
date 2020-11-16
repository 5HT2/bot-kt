package org.kamiblue.botkt.helpers

import org.kamiblue.botkt.*
import org.kamiblue.botkt.ConfigManager
import org.kamiblue.botkt.utils.MessageSendUtils.log
import org.kamiblue.botkt.UserConfig
import org.kamiblue.botkt.VersionConfig
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author l1ving
 * @since 2020/08/25 20:06
 */
object UpdateHelper {
    fun updateCheck() {
        if (File("noUpdateCheck").exists()) return
        val versionConfig =
            ConfigManager.readConfigFromUrl<VersionConfig>("https://raw.githubusercontent.com/kami-blue/bot-kt/master/version.json")

        if (versionConfig?.version == null) {
            log("Couldn't access remote version when checking for update")
            return
        }

        if (versionConfig.version != Main.currentVersion) {
            log("Not up to date:\nCurrent version: ${Main.currentVersion}\nLatest Version: ${versionConfig.version}")

            updateBot(versionConfig.version)
        } else {
            log("Up to date! Running on ${Main.currentVersion}")
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
            log("Auto Update - Deleted the following files:\n" + deleted.joinToString { it })
        }

        val bytes =
            URL("https://github.com/kami-blue/bot-kt/releases/download/$version/bot-kt-$version.jar").readBytes()

        log("Auto Update - Downloaded bot-kt-$version.jar ${bytes.size / 1000000}MB")
        val appendSlash = if (path.endsWith("/")) "" else "/"
        val targetFile = path.toString() + appendSlash + "bot-kt-$version.jar"
        File(targetFile).writeBytes(bytes)

        val file = Paths.get("$path/currentVersion")
        File(file.toString()).delete()
        Files.newBufferedWriter(file).use {
            it.write(version)
        }

        log("Auto Update - Finished updating to $version")

        userConfig.autoUpdateRestart?.let {
            if (it) {
                log("Auto Update - Restarting bot")
                Main.exit()
            }
        }
    }

    fun writeCurrentVersion() {
        val path = Paths.get(System.getProperty("user.dir"))
        val file = Paths.get("$path/currentVersion")

        if (!File(file.toString()).exists()) {
            Files.newBufferedWriter(file).use {
                it.write(Main.currentVersion)
            }
        }
    }
}
