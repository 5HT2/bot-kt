package org.kamiblue.botkt.utils

import net.ayataka.kordis.entity.message.Message
import org.kamiblue.botkt.config.global.GithubConfig

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
     * Will send an error in the if blank.
     * @return the default Github user in [GithubConfig]
     */
    suspend fun getDefaultGithubUser(message: Message?): String? {
        val user = GithubConfig.defaultGithubUser

        if (user.isBlank()) {
            message?.channel?.error("Github token not set in GithubConfig!")
            return null
        }

        return user
    }
}
