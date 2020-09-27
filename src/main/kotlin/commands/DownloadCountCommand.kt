package commands

import Colors
import Command
import ConfigManager.readConfigSafe
import Main
import PermissionTypes
import Permissions.hasPermission
import UserConfig
import doesLater
import org.l1ving.api.download.Download
import utils.*
import java.io.FileNotFoundException

object DownloadCountCommand : Command("downloadcount") {
    init {
        doesLater {
            try {
                if (!message.hasPermission(PermissionTypes.UPDATE_DOWNLOAD_COUNT)) {
                    return@doesLater
                }
                updateChannel()
                message.channel.send {
                    embed {
                        color = Colors.success
                        title = "Update success! Check the voice channel."
                    }
                }
            } catch (err: FileNotFoundException) {
                message.channel.send {
                    embed {
                        color = Colors.error
                        title = "Error! Something went wrong when executing this command!"
                        field("Stacktrace:", err.toString(), false)
                    }
                }
            }
        }
    }

    /**
     * @author sourTaste000
     * @since 9/22/2020
     * @throws FileNotFoundException
     */
    suspend fun updateChannel() {
        val server = readConfigSafe<UserConfig>(ConfigType.USER, false)?.primaryServerId?.let {
            Main.client?.servers?.find(it)
        } ?: run {
            return
        }
        val releaseChannel = readConfigSafe<UserConfig>(ConfigType.USER, false)?.downloadChannel?.let { server.voiceChannels.find(it) }
        val secondaryReleaseChannel = readConfigSafe<UserConfig>(ConfigType.USER, false)?.secondaryDownloadChannel?.let { server.voiceChannels.find(it) }
        val releaseCount = tokenRequest<Download>(getToken(null)
            ?: throw FileNotFoundException("Github token not find in config! Stopping..."),
            "https://api.github.com/repos/kami-blue/client/releases?per_page=200")
        val nightlyCount = tokenRequest<Download>(getToken(null)
            ?: throw FileNotFoundException("Github token not find in config! Stopping..."),
            "https://api.github.com/repos/kami-blue/nightly-releases/releases?per_page=200")
        var totalCount: Long = 0
        secondaryReleaseChannel?.let { it.edit { name = "${nightlyCount[0].assets[0].download_count} Nightly Downloads" } }
        for (i in nightlyCount) {
            for (j in i.assets) {
                totalCount += j.download_count
            }
        }
        for (i in releaseCount) {
            for (j in i.assets) {
                totalCount += j.download_count
            }
        }
        releaseChannel?.let { it.edit { name = "$totalCount Total Downloads" } }
    }
}