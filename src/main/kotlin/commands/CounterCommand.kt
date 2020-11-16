package org.kamiblue.botkt.commands

import net.ayataka.kordis.entity.server.Server
import org.kamiblue.botkt.*
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.utils.GitHubUtils
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.success
import org.kamiblue.botkt.utils.authenticatedRequest
import org.kamiblue.botkt.utils.request
import org.l1ving.api.download.Asset
import org.l1ving.api.download.Download

object CounterCommand : Command("counter") {
    init {
        doesLaterIfHas(PermissionTypes.UPDATE_COUNTERS) {
            val path = ConfigType.COUNTER.configPath.substring(7)
            val userPath = ConfigType.USER.configPath.substring(7)
            val config = readConfigSafe<CounterConfig>(ConfigType.COUNTER, false)

            if (config?.downloadEnabled != true && config?.memberEnabled != true) {
                message.error("Counters are not configured / enabled!")
            } else if (config.downloadEnabled != true) {
                message.error("Download counter is not enabled in the `$path` config!")
            } else if (config.memberEnabled != true) {
                message.error("Member counter is not enabled in the `$path` config!")
            }

            if (updateChannel()) {
                message.success("Successfully updated counters!")
            } else {
                message.error("Failed to update counters. Make sure `$path` is configured correctly, and `primaryServerId` is set in `$userPath`!")
            }
        }
    }

    /**
     * @author sourTaste000
     * @since 9/22/2020
     */
    suspend fun updateChannel(): Boolean {
        val config = readConfigSafe<CounterConfig>(ConfigType.COUNTER, false) ?: return false

        val server = readConfigSafe<UserConfig>(ConfigType.USER, false)?.primaryServerId?.let {
            Main.client.servers.find(it)
        } ?: run {
            return false
        }

        var downloadStable: Download? = null
        var downloadNightly: Download? = null

        var updated = false
        val perPage = config.perPage ?: 200

        GitHubUtils.getGithubToken(null)?.let {
            downloadStable = config.downloadStableUrl?.let { it1 -> authenticatedRequest<Download>("token", it, formatApiUrl(it1, perPage)) }
            downloadNightly = config.downloadNightlyUrl?.let { it1 -> authenticatedRequest<Download>("token", it, formatApiUrl(it1, perPage)) }
        } ?: run {
            downloadStable = config.downloadStableUrl?.let { request<Download>(formatApiUrl(it, perPage)) }
            downloadNightly = config.downloadNightlyUrl?.let { request<Download>(formatApiUrl(it, perPage)) }
        }

        var totalCount = 0
        var latestCount = 0
        val memberCount = server.members.size

        downloadStable?.let {
            totalCount += countedDownloads(it)
            latestCount = it[0].assets.count()
        }

        downloadNightly?.let {
            totalCount += countedDownloads(it)
            latestCount = it[0].assets.count() // nightly will be newer, so we assign it again if nightly isn't null
        }

        if (totalCount != 0 || latestCount != 0 || memberCount != 0) {
            edit(config, server, totalCount, latestCount, memberCount)
            updated = true
        }

        return updated
    }

    private suspend fun edit(config: CounterConfig, server: Server, totalCount: Int, latestCount: Int, memberCount: Int) {
        val totalChannel = config.downloadChannelTotal?.let { server.voiceChannels.find(it) }
        val latestChannel = config.downloadChannelLatest?.let { server.voiceChannels.find(it) }
        val memberChannel = config.memberChannel?.let { server.voiceChannels.find(it) }

        totalChannel?.let { it.edit { name = "$totalCount Downloads" } }
        latestChannel?.let { it.edit { name = "$latestCount Nightly DLs" } }
        memberChannel?.let { it.edit { name = "$memberCount Members" } }
    }

    private fun formatApiUrl(repo: String, perPage: Int) = "https://api.github.com/repos/$repo/releases?per_page=$perPage"

    private fun countedDownloads(download: Download): Int {
        var total = 0
        download.forEach { release ->
            total += release.assets.count()
        }
        return total
    }

    private fun List<Asset>.count(): Int {
        var total = 0
        this.forEach { total += it.download_count }
        return total
    }
}