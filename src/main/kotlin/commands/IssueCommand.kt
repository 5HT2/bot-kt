package commands

import AuthConfig
import Colors
import Command
import ConfigManager.readConfigSafe
import ConfigType
import Send.error
import UserConfig
import arg
import doesLater
import net.ayataka.kordis.entity.message.Message
import org.l1ving.api.issue.Issue
import org.l1ving.api.pull.PullRequest
import string
import utils.*

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
                        val githubToken = getToken(message) ?: return@doesLater // Error message is handled already
                        val user: String = context arg "user"
                        val repoName: String = context arg "repoName"
                        val issueNum: String = context arg "issueNum"

                        sendResponse(message, githubToken, user, repoName, issueNum)
                    }
                }
            }
        }
        string("repoName") {
            string("issueNum") {
                doesLater { context ->
                    val user: String = getUser(message) ?: return@doesLater // Error message is handled already
                    val githubToken = getToken(message) ?: return@doesLater
                    val repoName: String = context arg "repoName"
                    val issueNum: String = context arg "issueNum"

                    sendResponse(message, githubToken, user, repoName, issueNum)
                }
            }
        }
    }


    private suspend fun sendResponse(
        message: Message,
        token: String,
        user: String,
        repoName: String,
        issueNum: String
    ) {
        val issue = request<Issue>(token, "https://api.github.com/repos/$user/$repoName/issues/$issueNum")
        try {
            if (issue.html_url.contains("issue")) {
                //TODO: Duplicated code fragment
                message.channel.send {
                    embed {
                        title = issue.title
                        thumbnailUrl = issue.user.avatar_url
                        color = if (issue.state == "closed") Colors.error else Colors.success

                        field(
                            "Description",
                            if (issue.body?.isEmpty() == true) "No description" else issue.body!!.replace(
                                Regex("<!--.*-->"),
                                ""
                            ),
                            false
                        )

                        field("Milestone", issue.milestone?.title ?: "No Milestone", false)

                        field(
                            "Labels",
                            if ((issue.labels?.joinToString { it.name } ?: "No Labels").isEmpty()) {
                                "No Labels"
                            } else {
                                issue.labels!!.joinToString { it.name }
                            },
                            false
                        )

                        field(
                            "Assignees",
                            if ((issue.assignees?.joinToString { it.login } ?: "No Assignees").isEmpty()) {
                                "No Assignees"
                            } else {
                                issue.assignees!!.joinToString { it.login }
                            },
                            false
                        )

                        url = issue.html_url
                    }
                }
            } else if (issue.html_url.contains("pull")) {
                val pullRequest = request<PullRequest>(token, issue.url)

                //TODO: Duplicated code fragment
                message.channel.send {
                    embed {
                        title = pullRequest.title
                        thumbnailUrl = pullRequest.user.avatar_url
                        color = if (pullRequest.state == "closed") Colors.error else Colors.success

                        field(
                            "Description",
                            if (issue.body?.isEmpty() == true) "No description" else issue.body!!.replace(
                                Regex("<!--.*-->"),
                                ""
                            ),
                            false
                        )

                        field("Milestone", issue.milestone?.title ?: "No Milestone", false)

                        field(
                            "Labels",
                            if ((issue.labels?.joinToString { it.name } ?: "No Labels").isEmpty()) {
                                "No Labels"
                            } else {
                                issue.labels!!.joinToString { it.name }
                            },
                            false
                        )

                        field(
                            "Assignees",
                            if ((issue.assignees?.joinToString { it.login } ?: "No Assignees").isEmpty()) {
                                "No Assignees"
                            } else {
                                issue.assignees!!.joinToString { it.login }
                            },
                            false
                        )

                        field("Lines", "+${pullRequest.additions} / - ${pullRequest.deletions}", false)
                        field("Commits", pullRequest.commits, false)
                        field("Changed Files", pullRequest.changed_files, false)

                        url = pullRequest.url
                    }
                }
            }
        } catch (e: Exception) {
            message.channel.send {
                embed {
                    title = "Error"
                    description =
                        "Something went wrong when trying to execute this command! Does the user / repo / issue exist?"
                    field("Stacktrace", "```$e```", false)
                    e.printStackTrace()
                    color = Colors.error
                }
            }
        }
    }

    private suspend fun getToken(message: Message): String? {
        val token = readConfigSafe<AuthConfig>(ConfigType.AUTH, false)?.githubToken
        if (token == null) {
            message.error("Github Token not found!")
        }
        return token
    }

    private suspend fun getUser(message: Message): String? {
        val repo = readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultGithubUser
        if (repo == null) {
            message.error("Default user / org not set in `${ConfigType.USER.configPath.substring(7)}`!")
        }
        return repo
    }

    override fun getHelpUsage(): String {
        return "Getting information of an issue/pull on github. \n\n" +
                "Usage: \n" +
                "`;$name <user/organization> <repository> <issue>`\n\n" +
                "Example: \n" +
                "`;$name kami-blue bot-kt 10`\n\n"
    }
}