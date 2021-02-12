package org.kamiblue.botkt.utils

import net.ayataka.kordis.entity.message.Message
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.UserConfig
import org.kamiblue.botkt.config.global.GithubConfig
import org.kamiblue.botkt.manager.managers.ConfigManager

object GitHubUtils {
    /**
     * Will send an error in the channel if blank.
     * @return the Github token in [GithubConfig]
     */
    suspend fun getGithubToken(message: Message?): String? {
        val token = GithubConfig.githubToken

        if (token.isBlank()) {
            message?.channel?.error("Github token not set in GithubConfig!")
            return null
        }

        return token
    }

    /**
     * Will send an error in the [message]?.channel if null.
     * @return the default Github user, set in [UserConfig]
     */
    suspend fun getDefaultGithubUser(message: Message?): String? {
        val repo = ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultGithubUser
        if (repo == null) message?.channel?.error("Default user / org not set in `${ConfigType.USER.configPath.substring(7)}`!")
        return repo
    }
}
