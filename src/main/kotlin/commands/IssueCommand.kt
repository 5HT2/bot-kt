@file:Suppress("BlockingMethodInNonBlockingContext") //shut

package commands

import AuthConfig
import Command
import ConfigType
import FileManager
import Main
import arg
import classes.issue.Issue
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
        /* TODO: Make a tree that defaults to kami-blue for user */
        string("user") {
            string("repoName") {
                string("issueNum") {
                    doesLater { context ->
                        val user: String = context arg "user"
                        val repoName: String = context arg "repoName"
                        val issueNum: String = context arg "issueNum"
                        val githubToken =
                            FileManager.readConfigSafe<AuthConfig>(ConfigType.AUTH, true)?.githubToken ?: run {
                                message.channel.send {
                                    embed {
                                        title = "[$name]"
                                        description = "Github Token not found!"
                                        color = Main.Colors.ERROR.color
                                    }
                                }
                                return@doesLater
                            }
                        val request = Request.Builder().addHeader("Authorization", "token $githubToken").url("https://api.github.com/repos/$user/$repoName/issues/$issueNum").get().build()
                        val response = OkHttpClient().newCall(request).execute()
                        println(response.request().toString() + " from user ${message.author?.name}(${message.author?.id})")
                        val result = Gson().fromJson(response.body()!!.string(), Issue::class.java)
                        message.channel.send {
                            embed {
                                title = result!!.title
                                thumbnailUrl = result.user.avatar_url
                                color = if (result.state == "closed"){ Main.Colors.ERROR.color }else { Main.Colors.SUCCESS.color }
                                field("Description", if (result.body.isEmpty()) { "No description provided." } else { result.body.replace(Regex("<!--.*-->"), "") }, false)
                                field("Status", result.state, false)
                                field("Milestone", result.milestone.title, false)
                                author("カミブルー！", "https://kamiblue.org", "https://cdn.discordapp.com/avatars/743237292294013013/591c1daf9efcfdd7ea2db1592d818fa6.png")
                                url = result.html_url
                            }
                        }
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