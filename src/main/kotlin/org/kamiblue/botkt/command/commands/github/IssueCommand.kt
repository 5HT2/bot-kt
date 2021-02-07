package org.kamiblue.botkt.command.commands.github

import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.message.embed.EmbedBuilder
import org.kamiblue.botkt.*
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.*
import org.kamiblue.commons.extension.max
import org.l1ving.api.issue.Issue
import org.l1ving.api.pull.PullRequest
import java.awt.Color

/**
 * @author sourTaste000
 * @since 2020/9/8
 */
object IssueCommand : BotCommand(
    name = "issue",
    category = Category.GITHUB,
    description = "Fetch and create Github issues / pulls"
) {
    init {
        string("user") { user ->
            string("repoName") { repo ->
                int("issueNum") { issueNum ->
                    execute("Fetch a Github issue / pull") {
                        val githubToken = GitHubUtils.getGithubToken(message, "IssueCommand1") ?: return@execute // Error message is handled already

                        sendResponse(message, githubToken, user.value, repo.value, issueNum.value)
                    }
                }
            }
        }

        string("repoName") { repo ->
            int("issueNum") { issueNum ->
                execute("Fetch a Github issue / pull") {
                    val githubToken = GitHubUtils.getGithubToken(message, "IssueCommand2")
                        ?: return@execute // Error message is handled already
                    val user: String = GitHubUtils.getDefaultGithubUser(message) ?: return@execute

                    sendResponse(message, githubToken, user, repo.value, issueNum.value)
                }
            }
        }
    }

    private suspend fun sendResponse(
        message: Message,
        token: String,
        user: String,
        repoName: String,
        issueNum: Int
    ) {
        val issue = authenticatedRequest<Issue>("token", token, "https://api.github.com/repos/$user/$repoName/issues/$issueNum")

        if (issue.html_url != null && issue.html_url.contains("issue")) {
            message.channel.send {
                embed {
                    title = issue.title
                    thumbnailUrl = issue.user?.avatar_url
                    color = if (issue.state == "closed") Colors.ERROR.color else Colors.SUCCESS.color

                    commonFields(issue)

                    url = issue.html_url
                }
            }
        } else if (issue.html_url != null && issue.html_url.contains("pull")) {
            val pullRequest = authenticatedRequest<PullRequest>("token", token, issue.url!!)

            message.channel.send {
                embed {
                    title = pullRequest.title
                    thumbnailUrl = pullRequest.user?.avatar_url
                    color = getPullRequestColor(pullRequest)

                    commonFields(issue)

                    url = pullRequest.html_url
                }
            }
        } else {
            message.channel.error("Issue / pull `#$issueNum` in `$user/$repoName` could not be found!")
        }
    }

    private fun EmbedBuilder.commonFields(issue: Issue) {
        description = issue.body.defaultFromNull("No Description").max(2048)

        field("Milestone", issue.milestone?.title ?: "No Milestone", false)

        field(
            "Labels",
            nullOrBlankCheck(issue.labels?.mapNotNull { it.name }?.joinToString()),
            false
        )

        field(
            "Assignees",
            nullOrBlankCheck(issue.assignees?.mapNotNull { it.login }?.joinToString()),
            false
        )
    }

    private fun nullOrBlankCheck(string: String?): String {
        return if (string.isNullOrBlank() || string.isNullOrEmpty()) {
            "None"
        } else {
            string
        }
    }

    private fun getPullRequestColor(pullRequest: PullRequest): Color {
        return when {
            pullRequest.merged -> {
                Colors.MERGED_PULL_REQUEST.color
            }
            pullRequest.state == "closed" && !pullRequest.merged -> {
                Colors.ERROR.color
            }
            pullRequest.state == "open" -> {
                Colors.SUCCESS.color
            }
            else -> {
                Colors.WARN.color
            }
        }
    }

    private fun String?.defaultFromNull(default: String): String {
        return if (this?.isEmpty() != false) {
            default
        } else {
            this.replace(Regex("<!--.*-->"), "")
        }
    }
}
