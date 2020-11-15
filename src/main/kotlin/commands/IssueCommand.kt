package org.kamiblue.botkt.commands

import org.kamiblue.botkt.helpers.StringHelper.flat
import org.kamiblue.botkt.helpers.StringHelper.toHumanReadable
import kotlinx.coroutines.delay
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import net.ayataka.kordis.event.events.message.ReactionAddEvent
import org.kamiblue.botkt.*
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.Send.error
import org.kamiblue.botkt.Send.success
import org.l1ving.api.issue.Issue
import org.l1ving.api.issue.Label
import org.l1ving.api.issue.User
import org.l1ving.api.pull.PullRequest
import java.awt.Color

/**
 * @author sourTaste000
 * @since 2020/9/8
 */
object IssueCommand : Command("issue") {
    private val queuedIssues = HashMap<Long, Triple<Message, Issue, String>>()

    init {
        Main.client?.addListener(this)

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

        literal("create") {
            string("repo") {
                string("title") {
                    greedyString("body") {
                        doesLater { context ->
                            val repo: String = context arg "repo"
                            val title: String = context arg "title"
                            val body: String = context arg "body"

                            val issue = Issue()
                            val formattedIssue = "Created by: ${message.author?.name?.toHumanReadable()} `(${message.author?.id})`\n\n$body"

                            issue.title = title
                            issue.body = formattedIssue

                            val issueChannel = ConfigManager.readConfig<UserConfig>(ConfigType.USER, false)
                            issueChannel?.issueCreationChannel?.let {
                                if (it != message.channel.id) {
                                    message.error("You're only allowed to create issues in <#$it>!")
                                    return@doesLater
                                }
                            }

                            val user = ConfigManager.readConfig<UserConfig>(ConfigType.USER, false)?.defaultGithubUser ?: run {
                                message.error("Default Github User is not set in `${ConfigType.USER.configPath.substring(7)}`!")
                                return@doesLater
                            }

                            val form = message.channel.send {
                                embed {
                                    this.title = title
                                    this.description = "Created by: ${message.author?.mention}\n\n$body"

                                    field("Repository", "`$user/$repo`")
                                    color = Colors.primary
                                }
                            }

                            message.delete()

                            delay(1000)
                            form.addReaction('✅')
                            form.addReaction('⛔')

                            queuedIssues[form.id] = Triple(form, issue, repo)
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    suspend fun onReact(event: ReactionAddEvent) {
        if (!Main.ready) return

        val form = queuedIssues[event.reaction.messageId] ?: return

        var message = form.first

        if (event.reaction.emoji.name != "✅") {
            return
        } else if (event.reaction.emoji.name == "⛔") {
            message = message.error("Rejected issue ${form.second.title}!")
            form.first.delete()
            delay(5000)
            message.delete()
            return
        }

        if (!event.reaction.userId.hasPermission(PermissionTypes.APPROVE_ISSUE_CREATION)) return

        val token = ConfigManager.readConfig<AuthConfig>(ConfigType.AUTH, false)?.githubToken ?: run {
            message.error("Github Token is not set in `${ConfigType.AUTH.configPath.substring(7)}`!")
            return
        }

        val user = ConfigManager.readConfig<UserConfig>(ConfigType.USER, false)?.defaultGithubUser ?: run {
            message.error("Default Github User is not set in `${ConfigType.USER.configPath.substring(7)}`!")
            return
        }

        createGithubIssue(form.second, user, form.third, token)

        form.first.delete()
        message = message.success("Successfully created issue `${form.second.title}`!")
        delay(10000)
        message.delete()

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
        issueNum: String
    ) {
        val issue = authenticatedRequest<Issue>("token", token, "https://api.github.com/repos/$user/$repoName/issues/$issueNum")
        try {
            if (issue.html_url != null && issue.html_url!!.contains("issue")) {
                message.channel.send {
                    embed {
                        title = issue.title
                        thumbnailUrl = issue.user?.avatar_url
                        color = if (issue.state == "closed") Colors.error else Colors.success

                        description = issue.body.defaultFromNull("No Description").flat(2048)

                        field("Milestone", issue.milestone?.title ?: "No Milestone", false)

                        field(
                            "Labels",
                            issue.labels.joinToLabels(),
                            false
                        )

                        field(
                            "Assignees",
                            issue.assignees.joinToUsers(),
                            false
                        )

                        url = issue.html_url
                    }
                }
            } else if (issue.html_url != null && issue.html_url!!.contains("pull")) {
                val pullRequest = authenticatedRequest<PullRequest>("token", token, issue.url!!)

                message.channel.send {
                    embed {
                        title = pullRequest.title
                        thumbnailUrl = pullRequest.user?.avatar_url
                        color = getPullRequestColor(pullRequest)

                        description = issue.body.defaultFromNull("No Description").flat(2048)

                        field("Milestone", issue.milestone?.title ?: "No Milestone", false)

                        field(
                            "Labels",
                            issue.labels.joinToLabels(),
                            false
                        )

                        field(
                            "Assignees",
                            issue.assignees.joinToUsers(),
                            false
                        )

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
                    description =
                        "Something went wrong when trying to execute this command! Does the user / repo / issue exist?"
                    field("Stacktrace", "```${e.getStackTraceAsString()}```", false)
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

    private fun getPullRequestColor(pullRequest: PullRequest): Color {
        return when {
            pullRequest.merged -> {
                Colors.mergedPullRequest
            }
            pullRequest.state == "closed" && !pullRequest.merged -> {
                Colors.error
            }
            pullRequest.state == "open" -> {
                Colors.success
            }
            else -> {
                Colors.warn
            }
        }
    }

    private fun List<Label>?.joinToLabels(): String {
        val list = ArrayList<String>()

        this?.forEach {
            it.name?.let { name ->
                list.add(name)
            }
        }

        return if (list.isEmpty()) {
            "None"
        } else {
            list.joinToString()
        }
    }

    private fun List<User>?.joinToUsers(): String {
        val list = ArrayList<String>()

        this?.forEach {
            it.login?.let { login ->
                list.add(login)
            }
        }

        return if (list.isEmpty()) {
            "None"
        } else {
            list.joinToString()
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
