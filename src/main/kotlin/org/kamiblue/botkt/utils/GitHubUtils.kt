package org.kamiblue.botkt.utils

import com.google.gson.Gson
import net.ayataka.kordis.entity.message.Message
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.kamiblue.botkt.AuthConfig
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.UserConfig
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.l1ving.api.issue.Issue

object GitHubUtils {
    /**
     * Will send an error in the [message]?.channel if null.
     * @return the Github token, set in [AuthConfig]
     */
    suspend fun getGithubToken(message: Message?): String? {
        val token = ConfigManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, false)?.githubToken
        if (token == null) message?.error("Github token not set in `${ConfigType.AUTH.configPath.substring(7)}`!")
        return token
    }

    /**
     * Will send an error in the [message]?.channel if null.
     * @return the default Github user, set in [UserConfig]
     */
    suspend fun getDefaultGithubUser(message: Message?): String? {
        val repo = ConfigManager.readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultGithubUser
        if (repo == null) message?.error("Default user / org not set in `${ConfigType.USER.configPath.substring(7)}`!")
        return repo
    }

    /**
     * [user] and [repo] is the user/repo you want to create the issue in.
     * [token] is a Github Token with repo:public_repo checked
     */
    fun createGithubIssue(issue: Issue, user: String, repo: String, token: String) {
        val url = "https://api.github.com/repos/$user/$repo/issues"
        val body = Gson().toJson(issue).toRequestBody("".toMediaTypeOrNull())

        val request = Request.Builder()
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("Authorization", "token $token")
            .url(url).post(body).build()

        val response = OkHttpClient().newCall(request).execute()

        println(response.body?.string())
    }
}