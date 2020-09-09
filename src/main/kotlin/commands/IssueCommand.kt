@file:Suppress("BlockingMethodInNonBlockingContext") //shut

package commands

import AuthConfig
import Command
import classes.issue.Issue
import arg
import com.google.gson.Gson
import doesLater
import okhttp3.OkHttpClient
import okhttp3.Request
import string

/**
 * @author sourTaste000
 * @since 2020/9/8
 */
object IssueCommand : Command("issue") {
    init {
        string("user") {
            string("repoName") {
                string("issueNum") {
                    doesLater { context ->
                        val user: String = context arg "user"
                        val repoName: String = context arg "repoName"
                        val issueNum: String = context arg "issueNum"
                        val githubToken = FileManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, true)?.githubToken
                        val request = Request.Builder().addHeader("Authorization", "token $githubToken").url("https://api.github.com/repos/$user/$repoName/issues/$issueNum").get().build()
                        val response = OkHttpClient().newCall(request).execute()
                        println(response.request())
                        val result = Gson().fromJson(response.body()!!.string(), Issue::class.java)
                        message.channel.send(result!!.body)
                        }
                    }
                }
            }
        }

    override fun getHelpUsage(): String {
        return "Getting information of an issue on github. \n\n" +
                "Usage: \n" +
                "`;$name <user/organization> <repository> <issue>`\n\n" +
                "Example: \n" +
                "`;$name kami-blue bot-kt 10`\n\n"
    }
}