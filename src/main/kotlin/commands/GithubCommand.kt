package commands

import literal
import string
import Command
import Send.error
import Send.stackTrace
import arg
import doesLater
import authenticatedRequest
import getGithubToken
import org.l1ving.api.user.User
import java.lang.Exception

object GithubCommand : Command("github") {
    init {
        literal("user"){
            string("username") {
                doesLater { context ->
                    val username:String = context arg "username"
                    val token = getGithubToken(message)
                    val result = authenticatedRequest<User>("token", token ?: kotlin.run {
                        message.error("Github token not found in config!")
                        return@doesLater
                    }, "https://api.github.com/users/$username")
                    try {
                        message.channel.send {
                            embed {
                                title = "User: ${result.name ?: username}"
                                thumbnailUrl = result.avatar_url
                                field("Id:", result.id.toString())
                                field("Bio:", result.bio ?: "That user doesn't have a bio!")
                                field("Company:", result.company
                                        ?: "That user does not have a company in their profile!")
                                field("Followers:", result.followers.toString())
                                field("Following:", result.following.toString())
                                field("Hireable:", result.hireable.toString())
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