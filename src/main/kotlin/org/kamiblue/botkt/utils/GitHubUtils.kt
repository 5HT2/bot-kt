package org.kamiblue.botkt.utils

import com.google.gson.Gson
import net.ayataka.kordis.entity.message.Message
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.kamiblue.botkt.AuthConfig
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.UserConfig
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.l1ving.api.issue.Issue

object GitHubUtils {
    /**
     * Will send an error in the [message]?.channel if null.
     * @return the Github token, set in [AuthConfig]
     */
    suspend fun getGithubToken(message: Message?, scope: String? = null): String? {
        val token = ConfigManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, false)?.githubToken
        if (token == null) {
            message?.channel?.error("Github token not set in `${ConfigType.AUTH.configPath.substring(7)}`!")
        }

        scope?.let {
            Main.logger.debug("$scope called getGithubToken, token is ${if (token == null) "null" else "not null"}")
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
