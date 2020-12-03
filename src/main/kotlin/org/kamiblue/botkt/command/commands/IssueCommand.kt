package org.kamiblue.botkt.command.commands

import kotlinx.coroutines.delay
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.message.embed.EmbedBuilder
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import net.ayataka.kordis.event.events.message.ReactionAddEvent
import org.kamiblue.botkt.*
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.GitHubUtils
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.success
import org.kamiblue.botkt.utils.ReactionUtils.addReaction
import org.kamiblue.botkt.utils.StringUtils.flat
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.botkt.utils.authenticatedRequest
import org.l1ving.api.issue.Issue
import org.l1ving.api.pull.PullRequest
import java.awt.Color

/**
 * @author sourTaste000
 * @since 2020/9/8
 */
object IssueCommand : Command("issue") {
    private val queuedIssues = HashMap<Long, QueuedIssue>()

    init {
        Main.client.addListener(this)

        string("user") {
            string("repoName") {
                string("issueNum") {
                    doesLater { context ->
                        val githubToken =
                            GitHubUtils.getGithubToken(message)
                                ?: return@doesLater // Error message is handled already
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
                    val githubToken = GitHubUtils.getGithubToken(message)
                        ?: return@doesLater // Error message is handled already
                    val user: String = GitHubUtils.getDefaultGithubUser(message) ?: return@doesLater
                    val repoName: String = context arg "repoName"
                    val issueNum: String = context arg "issueNum"

                    sendResponse(message, githubToken, user, repoName, issueNum)
                }
            }
        }

        literal("create") {
            string("repo") {
                greedyString("contents") {
                    doesLater { context ->
                        val repo: String = context arg "repo"
                        val contents: String = context arg "contents"

                        val split = contents.split(';')
                        val title = split.getOrNull(0)
                        val body = split.getOrNull(1)

                        val formattedIssue = "Created by: ${message.author?.name?.toHumanReadable()} (${message.author?.mention})\n\n$body"
                        val issue = Issue(title = title, body = formattedIssue)

                        val issueChannel = ConfigManager.readConfig<UserConfig>(ConfigType.USER, false)
                        issueChannel?.issueCreationChannel?.let {
                            if (it != message.channel.id) {
                                message.error("You're only allowed to create issues in <#$it>!")
                                return@doesLater
                            }
                        }

                        val user = ConfigManager.readConfig<UserConfig>(ConfigType.USER, false)?.defaultGithubUser
                            ?: run {
                                message.error("Default Github User is not set in `${ConfigType.USER.configPath.substring(7)}`!")
                                return@doesLater
                            }

                        val form = message.channel.send {
                            embed {
                                this.title = title
                                this.description = "Created by: ${message.author?.mention}\n\n$body"

                                field("Repository", "`$user/$repo`")
                                color = Colors.PRIMARY.color
                            }
                        }

                        message.delete()

                        delay(500)
                        form.addReaction('✅')
                        delay(500)
                        form.addReaction('⛔')

                        queuedIssues[form.id] = QueuedIssue(form, issue, message.member, repo)
                    }
                }
            }
        }
    }

    @EventHandler
    suspend fun onReact(event: ReactionAddEvent) {
        if (!Main.ready || event.reaction.member?.bot == true) return
        if (!event.reaction.userId.hasPermission(PermissionTypes.APPROVE_ISSUE_CREATION)) return

        val form = queuedIssues[event.reaction.messageId] ?: return

        if (event.reaction.emoji.name == "✅") {
            var message = form.formMessage

            val token = ConfigManager.readConfig<AuthConfig>(ConfigType.AUTH, false)?.githubToken ?: run {
                message.error("Github Token is not set in `${ConfigType.AUTH.configPath.substring(7)}`!")
                return
            }

            val user = ConfigManager.readConfig<UserConfig>(ConfigType.USER, false)?.defaultGithubUser ?: run {
                message.error("Default Github User is not set in `${ConfigType.USER.configPath.substring(7)}`!")
                return
            }

            GitHubUtils.createGithubIssue(form.issue, user, form.repo, token)

            form.creator?.getPrivateChannel()?.send {
                embed {
                    title = form.issue.title
                    description = "Your suggestion / bug was accepted!"
                    field("Description:", form.issue.body ?: "")
                    field("Repository:", "$user/${form.repo}")
                    color = Colors.SUCCESS.color
                }

            }

            form.formMessage.delete()

            message = message.success("Successfully created issue `${form.issue.title}`!")

            delay(5000)

            message.delete()
        } else if (event.reaction.emoji.name == "⛔") {
            val message = form.formMessage

            val user = ConfigManager.readConfig<UserConfig>(ConfigType.USER, false)?.defaultGithubUser ?: run {
                message.error("Default Github User is not set in `${ConfigType.USER.configPath.substring(7)}`!")
                return
            }

            form.creator?.getPrivateChannel()?.send {
                embed {
                    title = form.issue.title
                    description = "Your suggestion / bug was rejected!"
                    field("Description:", form.issue.body ?: "")
                    field("Repository:", "$user/${form.repo}")
                    color = Colors.ERROR.color
                }
            }

            val feedback = message.error("Issue `${form.issue.title}` rejected!")

            delay(5000)
            message.delete()
            delay(5000)
            feedback.delete()
            queuedIssues.remove(event.reaction.messageId)
        }

    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
        if (!Main.ready || event.message.author?.bot == true) return

        if (event.message.author?.id?.hasPermission(PermissionTypes.APPROVE_ISSUE_CREATION) == true) return

        val issueChannel = ConfigManager.readConfig<UserConfig>(ConfigType.USER, false)
        issueChannel?.issueCreationChannel?.let {
            if (it != event.message.channel.id) return // only run the following code on messages in the issue channel
        } ?: run {
            return // issues are allowed inside any channel
        }

        if (event.message.content.isEmpty() || !event.message.content.startsWith("$fullName create")) {
            val reply = event.message.error("You need to use the `$fullName create` command to create an issue!")

            event.message.delete()
            delay(5000)
            reply.delete()
            return
        }
    }

    private suspend fun sendResponse(
        message: Message,
        token: String,
        user: String,
        repoName: String,
        issueNum: String,
    ) {
        val issue = authenticatedRequest<Issue>("token", token, "https://api.github.com/repos/$user/$repoName/issues/$issueNum")
        try {
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

                        field("Lines", "+${pullRequest.additions} / - ${pullRequest.deletions}", false)
                        field("Commits", pullRequest.commits ?: -1, false)
                        field("Changed Files", pullRequest.changed_files ?: -1, false)

                        url = pullRequest.html_url
                    }
                }
            }
        } catch (e: Exception) {
            message.channel.send {
                embed {
                    title = "Error"
                    description = "Something went wrong when trying to execute this command! Does the user / repo / issue exist?"
                    field("Stacktrace", "```${e.stackTraceToString()}```", false)
                    e.printStackTrace()
                    color = Colors.ERROR.color
                }
            }
        }
    }

    private fun EmbedBuilder.commonFields(issue: Issue) {
        description = issue.body.defaultFromNull("No Description").flat(2048)

        field("Milestone", issue.milestone?.title ?: "No Milestone", false)

        field(
            "Labels",
            issue.labels?.mapNotNull { it.name }?.joinToString() ?: "None",
            false
        )

        field(
            "Assignees",
            issue.assignees?.mapNotNull { it.login }?.joinToString() ?: "None",
            false
        )
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

    private data class QueuedIssue(
        val formMessage: Message,
        val issue: Issue,
        val creator: Member?,
        val repo: String,
    )

    override fun getHelpUsage(): String {
        return "Getting information of an issue/pull on github. \n\n" +
                "Usage: \n" +
                "`$fullName <user/organization> <repository> <issue>`\n\n" +
                "Example: \n" +
                "`$fullName kami-blue bot-kt 10`\n\n"
    }
}
