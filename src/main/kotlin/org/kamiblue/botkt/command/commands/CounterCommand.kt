package org.kamiblue.botkt.command.commands

import net.ayataka.kordis.entity.server.Server
import org.kamiblue.botkt.*
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.command.Command
import org.kamiblue.botkt.command.doesLaterIfHas
import org.kamiblue.botkt.utils.GitHubUtils
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.success
import org.kamiblue.botkt.utils.authenticatedRequest
import org.kamiblue.botkt.utils.request
import org.l1ving.api.download.Download

object CounterCommand : Command("counter") {
    init {
        doesLaterIfHas(PermissionTypes.UPDATE_COUNTERS) {
            val path = ConfigType.COUNTER.configPath.substring(7)
            val userPath = ConfigType.USER.configPath.substring(7)
            val config = readConfigSafe<CounterConfig>(ConfigType.COUNTER, false)

            when {
                config?.downloadEnabled != true && config?.memberEnabled != true -> {
                    message.error("Counters are not configured / enabled!")
                }
                config.downloadEnabled != true -> {
                    message.error("Download counter is not enabled in the `$path` config!")
                }
                config.memberEnabled != true -> {
                    message.error("Member counter is not enabled in the `$path` config!")
                }
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
        }
        val stableUrl = config.downloadStableUrl
        val nightlyUrl = config.downloadNightlyUrl
        val perPage = config.perPage ?: 200

        if (server == null || stableUrl == null || nightlyUrl == null) return false

        val downloads = GitHubUtils.getGithubToken(null)?.let { token ->
            authenticatedRequest<Download>("token", token, formatApiUrl(stableUrl, perPage)).countDownload() to // Stable
                authenticatedRequest<Download>("token", token, formatApiUrl(nightlyUrl, perPage)).countDownload() // Nightly
        } ?: run {
            request<Download>(formatApiUrl(stableUrl, perPage)).countDownload() to
                request<Download>(formatApiUrl(nightlyUrl, perPage)).countDownload()
        }

        val memberCount = server.members.size
        val totalDownload = downloads.first.first + downloads.second.first

        return if (totalDownload != 0 || memberCount != 0) {
            edit(config, server, totalDownload, downloads.second.second, memberCount)
            true
        } else {
            false
        }
    }

    private fun formatApiUrl(repo: String, perPage: Int) = "https://api.github.com/repos/$repo/releases?per_page=$perPage"

    private fun Download.countDownload(): Pair<Int, Int> {
        return this.sumBy { release -> release.assets.sumBy { it.download_count } } to
            this[0].assets.sumBy { it.download_count }
    }

    private suspend fun edit(config: CounterConfig, server: Server, totalCount: Int, latestCount: Int, memberCount: Int) {
        config.downloadChannelTotal?.let { server.voiceChannels.find(it)?.edit { name = "$totalCount Downloads" } }
        config.downloadChannelLatest?.let { server.voiceChannels.find(it)?.edit { name = "$latestCount Nightly DLs" } }
        config.memberChannel?.let { server.voiceChannels.find(it)?.edit { name = "$memberCount Members" } }
    }
}