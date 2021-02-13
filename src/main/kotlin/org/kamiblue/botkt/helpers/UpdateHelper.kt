package org.kamiblue.botkt.helpers

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.config.global.SystemConfig
import org.kamiblue.commons.utils.ConnectionUtils
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.nio.file.Paths

object UpdateHelper {
    private const val versionUrl = "https://raw.githubusercontent.com/kami-blue/bot-kt/master/version.json"

    fun updateCheck() {
        if (File("noUpdateCheck").exists()) return
        val response = ConnectionUtils.requestRawJsonFrom(versionUrl)

        if (response == null) {
            Main.logger.warn("Failed to request version info")
            return
        }

        val version = try {
            JsonParser.parseString(response).asJsonObject["version"].asString!!
        } catch (e: Exception) {
            Main.logger.warn("Failed to parser version info json", e)
            return
        }

        if (version != Main.currentVersion) {
            Main.logger.info("Not up to date:")
            Main.logger.info("Current version: ${Main.currentVersion}")
            Main.logger.info("Latest Version: $version")

            updateBot(version)
        } else {
            Main.logger.info("Up to date! Running on ${Main.currentVersion}")
        }
    }

    private fun updateBot(version: String) {
        if (!SystemConfig.autoUpdate) {
            return
        }

        runBlocking {
            val deferred = async(Dispatchers.IO) {
                val url = URL("https://github.com/kami-blue/bot-kt/releases/download/$version/bot-kt-$version.jar")
                url.readBytes()
            }

            val path = System.getProperty("user.dir")!!

            launch(Dispatchers.IO) {
                deleteOldJars(path)
            }

            val slashRemoved = path.removeSuffix("/")
            val targetFile = "$slashRemoved/bot-kt-$version.jar"

            val bytes = deferred.await()
            Main.logger.info("Auto Update - Downloaded bot-kt-$version.jar ${bytes.size / 1000000}MB")

            File(targetFile).writeBytes(bytes)
            writeVersion(version)

            Main.logger.info("Auto Update - Finished updating to $version")

            if (SystemConfig.autoUpdateRestart) {
                Main.logger.info("Auto Update - Restarting bot")
                Main.exit()
            }
        }
    }

    private fun deleteOldJars(path: String) {
        val deleted = ArrayList<String>()
        val regex = Regex("bot-kt.*.jar", RegexOption.IGNORE_CASE)

        File(path).listFiles()?.forEach {
            if (it.name.matches(regex)) {
                deleted.add(it.name)
                it.delete()
            }
        }

        if (deleted.isNotEmpty()) {
            Main.logger.info("Auto Update - Deleted the following files:\n" + deleted.joinToString())
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
