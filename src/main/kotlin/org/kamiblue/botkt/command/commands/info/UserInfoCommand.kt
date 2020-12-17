package org.kamiblue.botkt.command.commands.info

import com.google.gson.JsonParser
import net.ayataka.kordis.DiscordClientImpl
import net.ayataka.kordis.entity.findByTag
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.entity.user.UserImpl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageUtils.error
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.botkt.utils.StringUtils.toUserID
import org.kamiblue.botkt.utils.accountAge
import org.kamiblue.botkt.utils.getAuthToken
import org.kamiblue.botkt.utils.prettyFormat

object UserInfoCommand : BotCommand(
    name = "userinfo",
    alias = arrayOf("whois"),
    category = Category.INFO,
    description = "Look up info for a Discord user"
) {

    private const val current = "Not in current guild!"

    init {
        execute("Get info for yourself") {
            val username: String = message.author?.id?.toString() ?: run {
                message.author?.tag ?: run {
                    message.channel.error("Couldn't find your user, try using a direct ID!")
                    return@execute
                }
            }
            send(username, message)
        }

        greedy("name") { nameArg ->
            execute("Find a user with their name, a ping or their ID") {
                val name = nameArg.value
                send(name, message)
            }
        }
    }

    private suspend fun send(username: String, message: Message) {
        val member: Member? = username.toUserID()?.let {
            message.server?.members?.find(it)
        } ?: message.server?.members?.findByTag(username)
        ?: message.server?.members?.findByName(username)

        member?.let {
            message.channel.send {
                embed {
                    title = it.tag
                    color = Colors.PRIMARY.color
                    thumbnailUrl = it.avatar.url

                    field("Created Account:", it.timestamp.prettyFormat())
                    field("Joined Guild:", it.joinedAt.prettyFormat())
                    field("Join Age:", it.accountAge().toString() + " days")
                    field("Account Age:", it.accountAge().toString() + " days")
                    field("Mention:", it.mention)
                    field("ID:", "`${it.id}`")
                    field("Status:", it.status.name.toHumanReadable())
                }
            }

        } ?: run {
            val id = username.toUserID() ?: run {
                message.channel.error("Couldn't find user nor a valid ID!")
                return
            }

            val user = requestUser(id)

            message.channel.send {
                embed {
                    title = user.tag
                    color = Colors.PRIMARY.color
                    thumbnailUrl = user.avatar.url

                    field("Created Account:", user.timestamp.prettyFormat())
                    field("Joined Guild:", current)
                    field("Join Age:", current)
                    field("Account Age:", user.accountAge().toString() + " days")
                    field("Mention:", user.mention)
                    field("ID:", "`${user.id}`")
                    field("Status:", current)
                }
            }
        }
    }

    private fun requestUser(id : Long) : User {
        val request = Request.Builder().addHeader("Authorization", "Bot ${getAuthToken()}").url("https://discord.com/api/v8/users/$id").get().build()
        val response = OkHttpClient().newCall(request).execute()
        val jsonObject = JsonParser.parseString(response.body?.string()).asJsonObject
        return UserImpl(Main.client as DiscordClientImpl, jsonObject)
    }

}
