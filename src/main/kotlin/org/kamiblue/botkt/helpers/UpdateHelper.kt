package org.kamiblue.botkt.helpers

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.VersionConfig
import org.kamiblue.botkt.config.global.SystemConfig
import org.kamiblue.botkt.manager.managers.ConfigManager
import java.io.File
import java.io.FileWriter
import java.net.URL
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
            Main.logger.info("Couldn't access remote version when checking for update")
            return
        }

        if (versionConfig.version != Main.currentVersion) {
            Main.logger.info("Not up to date:")
            Main.logger.info("Current version: ${Main.currentVersion}")
            Main.logger.info("Latest Version: ${versionConfig.version}")

            updateBot(versionConfig.version)
        } else {
            Main.logger.info("Up to date! Running on ${Main.currentVersion}")
        }
    }

    private fun updateBot(version: String) {
        if (!SystemConfig.autoUpdate) {
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
            Main.logger.info("Auto Update - Deleted the following files:\n" + deleted.joinToString())
        }

        val bytes =
            URL("https://github.com/kami-blue/bot-kt/releases/download/$version/bot-kt-$version.jar").readBytes()

        Main.logger.info("Auto Update - Downloaded bot-kt-$version.jar ${bytes.size / 1000000}MB")
        val appendSlash = if (path.endsWith("/")) "" else "/"
        val targetFile = path.toString() + appendSlash + "bot-kt-$version.jar"
        File(targetFile).writeBytes(bytes)

        writeVersion(version)

        Main.logger.info("Auto Update - Finished updating to $version")

        if (SystemConfig.autoUpdateRestart) {
            Main.logger.info("Auto Update - Restarting bot")
            Main.exit()
        }
    }

    fun writeVersion(version: String) {
        val path = Paths.get(System.getProperty("user.dir"))
        val file = File("$path/currentVersion")

        FileWriter(file).use {
            it.write(version)
        }
    }
}
