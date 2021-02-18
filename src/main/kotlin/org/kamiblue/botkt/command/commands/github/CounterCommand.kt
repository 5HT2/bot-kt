package org.kamiblue.botkt.command.commands.github

import net.ayataka.kordis.entity.server.Server
import org.kamiblue.botkt.BackgroundScope
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.options.HasPermission
import org.kamiblue.botkt.config.global.CounterConfig
import org.kamiblue.botkt.config.global.SystemConfig
import org.kamiblue.botkt.utils.*
import org.l1ving.api.download.Download

// TODO: Get the github download counter out of this and make this server dependent
object CounterCommand : BotCommand(
    name = "counter",
    category = Category.GITHUB,
    description = "Count members and Github repo downloads"
) {
    init {
        execute(HasPermission.get(PermissionTypes.UPDATE_COUNTERS)) {
=
            when {
                !CounterConfig.downloadCounter && !CounterConfig.memberCounter -> {
                    channel.warn("Counters are not enabled!")
                }
                !CounterConfig.downloadCounter -> {
                    channel.warn("Download counter is not enabled in the `Counter` config!")
                }
                !CounterConfig.memberCounter -> {
                    channel.warn("Member counter is not enabled in the `Counter` config!")
                }
            }

            if (updateChannel()) {
                channel.success("Successfully updated counters!")
            } else {
                channel.error("Failed to update counters. Make sure `CounterConfig` is configured correctly, and `startupServer` is set in `SystemConfig`!")
            }
        }

        BackgroundScope.launchLooping("Counter channel update", 600000) {
            updateChannel()
            Main.logger.debug("Updated counter channels")
        }
    }

    private suspend fun updateChannel(): Boolean {
        val server = Main.client.servers.find(SystemConfig.startupServer) ?: return false

        val stableUrl = formatApiUrl(CounterConfig.stableRepo)
        val nightlyUrl = formatApiUrl(CounterConfig.nightlyRepo)

        val downloads = GitHubUtils.getGithubToken(null)?.let { token ->
            authenticatedRequest<Download>("token", token, nightlyUrl).countDownload() to // Stable
                authenticatedRequest<Download>("token", token, stableUrl).countDownload() // Nightly
        } ?: run {
            request<Download>(stableUrl).countDownload() to
                request<Download>(nightlyUrl).countDownload()
        }

        val memberCount = server.members.size
        val totalDownload = downloads.first.first + downloads.second.first

        return if (totalDownload != 0 || memberCount != 0) {
            edit(server, totalDownload, downloads.second.second, memberCount)
            true
        } else {
            false
        }
    }

    private fun formatApiUrl(repo: String) =
        "https://api.github.com/repos/$repo/releases?per_page=${CounterConfig.perPage}"

    private fun Download.countDownload(): Pair<Int, Int> {
        return this.sumBy { release -> release.assets.sumBy { it.download_count } } to
            this[0].assets.sumBy { it.download_count }
    }

    private suspend fun edit(
        server: Server,
        totalCount: Int,
        latestCount: Int,
        memberCount: Int
    ) {
        if (CounterConfig.totalDownloadChannel != -1L) {
            server.voiceChannels.find(CounterConfig.totalDownloadChannel)?.edit {
                name = "$totalCount Downloads"
            }
        }

        if (CounterConfig.nightlyDownloadChannel != -1L) {
            server.voiceChannels.find(CounterConfig.nightlyDownloadChannel)?.edit {
                name = "$latestCount Nightly DLs"
            }
        }

        if (CounterConfig.memberChannel != -1L) {
            server.voiceChannels.find(CounterConfig.memberChannel)?.edit {
                name = "$memberCount Members"
            }
        }
    }
}
