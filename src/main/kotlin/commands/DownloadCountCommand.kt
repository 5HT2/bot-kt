package commands

import Command
import Permissions.hasPermission
import doesLater
import org.l1ving.api.download.Download
import utils.*
import java.io.FileNotFoundException

object DownloadCountCommand : Command("downloadcount"){
    init{
        doesLater{
            try{
                if (!message.hasPermission(PermissionTypes.FORCE_UPDATE)) {return@doesLater}
                updateChannel()
                message.channel.send {
                    embed {
                        color = Colors.success
                        title = "Update success! Check the voice channel."
                    }
                }
            }catch(err: FileNotFoundException){
                message.channel.send{
                    embed{
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
        val server = Main.client?.servers?.find(getServerId())
        val releaseChannel = server?.voiceChannels?.find(getReleaseChannel())
        val secondaryReleaseChannel = server?.voiceChannels?.find(getSecondaryReleaseChannel())
        val releaseCount = tokenRequest<Download>(getToken(null) ?: throw FileNotFoundException("Github token not find in config! Stopping..."), "https://api.github.com/repos/kami-blue/client/releases?per_page=200")
        val nightlyCount = tokenRequest<Download>(getToken(null) ?: throw FileNotFoundException("Github token not find in config! Stopping..."), "https://api.github.com/repos/kami-blue/nightly-releases/releases?per_page=200")
        var totalCount: Long = 0
        secondaryReleaseChannel?.edit { name = "${nightlyCount[0].assets[0].download_count} Nightly Downloads" }
        for(i in nightlyCount){ for(j in i.assets){ totalCount += j.download_count } }
        for(i in releaseCount){ for(j in i.assets){ totalCount += j.download_count } }
        releaseChannel?.edit { name = "$totalCount Total Downloads" }
    }
}