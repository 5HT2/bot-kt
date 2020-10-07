package commands

import Colors
import Command
import Send.stackTrace
import helpers.StringHelper.flat
import arg
import authenticatedRequest
import doesLater
import getDefaultGithubUser
import getGithubToken
import net.ayataka.kordis.entity.message.Message
import org.l1ving.api.issue.Issue
import org.l1ving.api.pull.PullRequest
import string
import java.awt.Color

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
                        val githubToken =
                            getGithubToken(message) ?: return@doesLater // Error message is handled already
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
                    val githubToken = getGithubToken(message) ?: return@doesLater // Error message is handled already
                    val user: String = getDefaultGithubUser(message) ?: return@doesLater
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
        val issue = authenticatedRequest<Issue>("token", token, "https://api.github.com/repos/$user/$repoName/issues/$issueNum")
        try {
            if (issue.html_url.contains("issue")) {
                message.channel.send {
                    embed {
                        title = issue.title
                        thumbnailUrl = issue.user.avatar_url
                        color = if (issue.state == "closed") Colors.error else Colors.success

                        description = if (issue.body?.isEmpty() == true) "No description" else issue.body!!.replace(
                            Regex("<!--.*-->"),
                            ""
                        ).flat(2048)

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
                val pullRequest = authenticatedRequest<PullRequest>("token", token, issue.url)
                message.channel.send {
                    embed {
                        title = pullRequest.title
                        thumbnailUrl = pullRequest.user.avatar_url
                        color = getPullRequestColor(pullRequest)

                        description = if (issue.body?.isEmpty() == true) "No description" else issue.body!!.replace(
                            Regex("<!--.*-->"),
                            ""
                        ).flat(2048)

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

                        url = pullRequest.html_url
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

    override fun getHelpUsage(): String {
        return "Getting information of an issue/pull on github. \n\n" +
                "Usage: \n" +
                "`$fullName <user/organization> <repository> <issue>`\n\n" +
                "Example: \n" +
                "`$fullName kami-blue bot-kt 10`\n\n"
    }

    private fun getPullRequestColor( pullRequest: PullRequest): Color {
        return when {
            pullRequest.state == "closed" && !pullRequest.merged -> { Colors.error }
            pullRequest.merged -> { Colors.merged }
            pullRequest.state == "open" -> { Colors.success }
            else -> { Colors.warn }
        }
    }
}
