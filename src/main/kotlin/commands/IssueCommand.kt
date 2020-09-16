package commands

import AuthConfig
import Command
import ConfigType
import FileManager
import Main
import arg
import com.google.gson.Gson
import doesLater
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kamiblue.api.issue.Issue
import org.kamiblue.api.pull.PullRequest
import string

/**
 * @author sourTaste000
 * @since 2020/9/8
 */
@Suppress("BlockingMethodInNonBlockingContext")
object IssueCommand : Command("issue") {
    init {
        /*
        TODO:
             Make a tree that defaults to kami-blue for user
             Support Multiple Assignees
         */
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
                        val request = Request.Builder().addHeader("Authorization", "token $githubToken")
                            .url("https://api.github.com/repos/$user/$repoName/issues/$issueNum").get().build()
                        val response = OkHttpClient().newCall(request).execute()
                        println(
                            response.request().toString() + " from user ${message.author?.name}(${message.author?.id})"
                        )
                        val result = Gson().fromJson(response.body()!!.string(), Issue::class.java)
                        try {
                            if (result.html_url.contains("issue")) {
                                message.channel.send {
                                    embed {
                                        title = result!!.title
                                        thumbnailUrl = result.user.avatar_url
                                        color = if (result.state == "closed") {
                                            Main.Colors.ERROR.color
                                        } else {
                                            Main.Colors.SUCCESS.color
                                        }
                                        field(
                                            "Description",
                                            if (result.body?.isEmpty() == true) {
                                                "No description provided."
                                            } else {
                                                result.body!!.replace(Regex("<!--.*-->"), "")
                                            }, false
                                        )
                                        field("Status", result.state, false)
                                        field("Milestone", result.milestone?.title ?: "No Milestone Added", false)
                                        field(
                                            "Labels",
                                            if ((result.labels?.joinToString { it.name } ?: "No Labels").isEmpty()) {
                                                "No Labels"
                                            } else {
                                                result.labels!!.joinToString { it.name }
                                            },
                                            false
                                        )
                                        field(
                                            "Assignees",
                                            if ((result.assignees?.joinToString { it.login }
                                                    ?: "No Assignees").isEmpty()) {
                                                "No Assignees"
                                            } else {
                                                result.assignees!!.joinToString { it.login }
                                            },
                                            false
                                        )
                                        author(
                                            "カミレッドー！",
                                            "https://kamiblue.org",
                                            "https://cdn.discordapp.com/avatars/743237292294013013/591c1daf9efcfdd7ea2db1592d818fa6.png"
                                        )
                                        url = result.html_url
                                    }
                                }
                            } else if (result.html_url.contains("pull")) {
                                val requestPull =
                                    Request.Builder().addHeader("Authorization", "token $githubToken").url(result.url)
                                        .get().build()
                                val responsePull = OkHttpClient().newCall(requestPull).execute()
                                println(
                                    responsePull.request()
                                        .toString() + " from user ${message.author?.name}(${message.author?.id})"
                                )
                                val resultPull =
                                    Gson().fromJson(responsePull.body()!!.string(), PullRequest::class.java)
                                message.channel.send {
                                    embed {
                                        title = resultPull.title
                                        thumbnailUrl = resultPull.user.avatar_url
                                        color = if (resultPull.state == "closed") {
                                            Main.Colors.ERROR.color
                                        } else {
                                            Main.Colors.SUCCESS.color
                                        }
                                        field(
                                            "Description",
                                            if (resultPull.body.isEmpty()) {
                                                "No description provided."
                                            } else {
                                                resultPull.body.replace(Regex("<!--.*-->"), "")
                                            }, false
                                        )
                                        field("Additions", resultPull.additions, false)
                                        field("Deletions", resultPull.deletions, false)
                                        field("Commits", resultPull.commits, false)
                                        field("Changed Files", resultPull.changed_files, false)
                                        field("Comments", resultPull.comments, false)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            message.channel.send {
                                embed {
                                    title = "Error"
                                    description =
                                        "Something went wrong when trying to execute this command! Does the user/repo/issue exist?"
                                    field("Stacktrace", "```$e```", false)
                                    e.printStackTrace()
                                    color = Main.Colors.ERROR.color
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Getting information of an issue/pull on github. \n\n" +
                "Usage: \n" +
                "`;$name <user/organization> <repository> <issue>`\n\n" +
                "Example: \n" +
                "`;$name kami-blue bot-kt 10`\n\n"
    }
}