package commands

import Command
import Send.error
import Send.stackTrace
import arg
import authenticatedRequest
import doesLater
import getGithubToken
import literal
import org.l1ving.api.commit.Commit
import org.l1ving.api.user.User
import string

object GithubCommand : Command("github") {
    init {
        literal("user") {
            string("username") {
                doesLater { context ->
                    val username: String = context arg "username"
                    val token = getGithubToken(message)
                    try {
                        val result = authenticatedRequest<User>("token", token ?: kotlin.run {
                            message.error("Github token not found in config!")
                            return@doesLater
                        }, "https://api.github.com/users/$username")
                        message.channel.send {
                            embed {
                                title = "User: ${result.name ?: username}"
                                thumbnailUrl = result.avatar_url
                                url = result.html_url
                                color = Colors.primary
                                field("Id:", result.id.toString())
                                field("Bio:", result.bio ?: "That user doesn't have a bio!")
                                field(
                                    "Company:", result.company
                                        ?: "That user does not have a company in their profile!"
                                )
                                field("Followers:", result.followers.toString())
                                field("Following:", result.following.toString())
                                field("Hireable:", result.hireable.toString())
                            }
                        }
                    } catch (e: Exception) {
                        message.stackTrace(e)
                    }
                }
            }
        }
        literal("commit") {
            string("sha") {
                doesLater { context ->
                    val sha: String = context arg "sha"
                    val token = getGithubToken(message)
                    try {
                        val result = authenticatedRequest<Commit>("token", token ?: kotlin.run {
                            message.error("Github token not found in config!")
                            return@doesLater
                        }, "https://api.github.com/repos/kami-blue/bot-kt/git/commits/$sha")
                        message.channel.send{
                            embed {
                                title = result.message
                                url = result.html_url
                                description = result.verification?.payload
                                color = Colors.primary
                                field("Signature:", "```${result.verification?.signature ?: "No Signature"}```")
                            }
                        }
                    } catch(e: Exception){
                        message.stackTrace(e)
                    }
                }
            }
        }
    }
}