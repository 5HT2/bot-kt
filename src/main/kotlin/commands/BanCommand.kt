package org.kamiblue.botkt.commands

import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.user.User
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.kamiblue.botkt.*
import org.kamiblue.botkt.ConfigManager.readConfigSafe
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.normal
import org.kamiblue.botkt.utils.ReactionUtils.FakeUser
import org.kamiblue.botkt.utils.StringUtils.flat
import org.kamiblue.botkt.utils.StringUtils.toUserID
import org.kamiblue.botkt.utils.authenticatedRequest
import org.kamiblue.botkt.utils.getAuthToken

object BanCommand : Command("ban") {
    private const val banReason = "Ban Reason:"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        literal("regex") {
            literal("confirm") {
                greedyString("userRegex") {
                    doesLaterIfHas(PermissionTypes.MASS_BAN) { context ->
                        val regexIn: String = context arg "userRegex"

                        val server = server ?: run { message.error("Server members are null, are you running this from a DM?"); return@doesLaterIfHas }

                        val m = message.error("Banning [calculating] members...")

                        var banned = 0
                        val regex = regexIn.toRegex()
                        val reason = "Mass ban by ${message.author?.name}#${message.author?.discriminator}"
                        val filtered = server.members.filter { it.name.contains(regex) }

                        if (filtered.isEmpty()) {
                            m.edit {
                                description = "Not banning anybody! 0 members found."
                                color = Colors.ERROR.color
                            }
                            return@doesLaterIfHas
                        } else {
                            m.edit {
                                description = "Banning ${filtered.size} members..."
                                color = Colors.ERROR.color
                            }
                        }

                        filtered.forEach {
                            banned++
                            ban(it, 1, false, reason, server, message)
                            delay(200)
                        }

                        m.edit {
                            field(
                                "$banned members were banned by:",
                                message.author?.mention.toString(),
                                false
                            )
                            field(
                                banReason,
                                reason,
                                false
                            )
                            footer("ID: ${message.author?.id}", "https://cdn.discordapp.com/avatars/${message.author?.id}/${message.author?.avatar}.png")
                            color = Colors.ERROR.color
                        }
                    }
                }
            }

            greedyString("userRegex") {
                doesLaterIfHas(PermissionTypes.MASS_BAN) { context ->
                    val regexIn: String = context arg "userRegex"
                    val regex = regexIn.toRegex()

                    val members = server?.members ?: run {
                        message.error("Server members are null, are you running this from a DM?")
                        return@doesLaterIfHas
                    }

                    val filtered = members.filter { it.name.contains(regex) }.joinToString(separator = "\n") { it.mention }
                    val final = if (filtered.length > 2048) filtered.flat(1998) + "\nNot all users are shown, due to size limitations." else filtered

                    if (members.isEmpty()) {
                        message.error("Couldn't find any members that match the regex `$regexIn`!")
                    } else {
                        message.normal(final)
                    }
                }
            }
        }

        greedyString("userAndReason") {
            doesLaterIfHas(PermissionTypes.COUNCIL_MEMBER) { context -> // we unfortunately have to do really icky manual string parsing here, due to brigadier not knowing <@!id> is a string
                val username: String = context arg "userAndReason"

                if (!username.contains(" ")) {
                    ban(username, false, null, server, message)
                    return@doesLaterIfHas
                } else {
                    // split message in the format of [username, false/true, reason]
                    val splitWithDeleteMsgs = username.split(" ".toRegex(), 3)
                    val deleteMsgsReason = splitWithDeleteMsgs.getOrNull(2)

                    if (splitWithDeleteMsgs[1].equals("true", true)) { // [username, *true*, reason]
                        ban(splitWithDeleteMsgs[0], true, deleteMsgsReason, server, message)
                        return@doesLaterIfHas
                    } else if (splitWithDeleteMsgs[1].equals("false", true)) { // [username, *false*, reason]
                        ban(splitWithDeleteMsgs[0], false, deleteMsgsReason, server, message)
                        return@doesLaterIfHas
                    }

                    // split message in the format of [username, reason], provided username does not contain spaces (it shouldn't!!)
                    val split = username.split(" ".toRegex(), 2)
                    if (split.size != 2) {
                        message.error("Failed to ban $username, this should not be possible. Size: `${split.size}`")
                        return@doesLaterIfHas
                    }
                    ban(split[0], false, split[1], server, message)
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun ban(
        unfilteredUsername: String, // this can be an @mention (<@id>), an ID (id), or a username (username#discrim)
        deleteMsgs: Boolean, // if we should delete the past day of their messages or not
        reason: String?, // reason why they were banned. dmed before banning
        nullableServer: Server?,
        message: Message
    ) {
        val server = nullableServer ?: run { message.error("Server is null, make sure you aren't running this from a DM!"); return }

        var username = unfilteredUsername
        var usernameIsId = false
        val deleteMessageDays = if (deleteMsgs) 1 else 0
        val fixedReason = if (reason != null && reason.isNotEmpty()) reason else readConfigSafe<UserConfig>(ConfigType.USER, false)?.defaultBanReason ?: "No Reason Specified"

        username.toUserID()?.let {
            username = it.toString()
            usernameIsId = true
        }

        val user: User = server.members.findByName(username) ?: server.members.find(username.toLong()) ?: // ID, or ping with the regex [<@!>] removed
        run {
            banIfNotFound(usernameIsId, username, unfilteredUsername, deleteMessageDays, fixedReason, server, message)
            return // required
        }

        ban(user, deleteMessageDays, true, reason, server, message)

    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun banIfNotFound(usernameIsId: Boolean, username: String, unfilteredUsername: String, deleteMessageDays: Int, fixedReason: String, server: Server, message: Message) {
        if (!usernameIsId) {
            val nor: String = if (username != unfilteredUsername) "$unfilteredUsername nor $username" else unfilteredUsername
            message.error("User $nor not found!")
            return
        }

        try { // attempt to ban user even if they aren't in the guild
            if (username.toLong() == message.author?.id) {
                message.error("You can't ban yourself!")
                return
            } else if (username.toLong().hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
                message.error("That user is protected, I can't do that.")
                return
            }

            val req = "{\"delete_message_days\":\"$deleteMessageDays\", \"reason\":\"$fixedReason\"}"

            val json: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body: RequestBody = req.toRequestBody(json)
            val request = Request.Builder()
                .addHeader("Authorization", "Bot ${getAuthToken()}")
                .put(body)
                .url("https://discord.com/api/v6/guilds/${server.id}/bans/$username")
                .build()

            val response = OkHttpClient().newCall(request).execute()

            if (response.body?.string()?.count() == 0) {
                val user = authenticatedRequest<FakeUser>("Bot", getAuthToken(), "https://discord.com/api/v8/users/$username")
                message.channel.send {
                    embed {
                        field(
                            "${user.username}#${user.discriminator} was banned by:",
                            message.author!!.mention,
                            false
                        )
                        field(
                            banReason,
                            fixedReason,
                            false
                        )
                        footer("ID: ${user.id}", "https://cdn.discordapp.com/avatars/$username/${user.avatar}.png")
                        color = Colors.ERROR.color
                    }
                }
            } else {
                val prettyResponse = gson.toJson(response.body?.string())
                message.channel.send {
                    embed {
                        title = "Failed to ban user!"
                        field("Response:", "```json$prettyResponse```")
                        color = Colors.ERROR.color
                    }
                }
            }
        } catch (e: Exception) {
            message.channel.send {
                embed {
                    title = "That user's role is higher then mine, I can't ban them!"
                    field("Stacktrace:", "```$e```")
                    color = Colors.ERROR.color
                }
            }
        }
    }

    private suspend fun ban(
        user: User,
        deleteMessageDays: Int,
        sendReasonFeedback: Boolean, // if false, will not send feedback in channel. will not dm user
        reason: String?,
        server: Server,
        message: Message
    ) {
        if (user.id == message.author?.id) {
            message.error("You can't ban yourself!")
            return
        } else if (user.id.hasPermission(PermissionTypes.COUNCIL_MEMBER)) {
            message.error("That user is protected, I can't do that.")
            return
        }

        if (sendReasonFeedback) {
            try {
                user.getPrivateChannel().send {
                    embed {
                        field(
                            "You were banned by:",
                            message.author!!.mention,
                            false
                        )
                        field(
                            banReason,
                            reason ?: "None Provided",
                            false
                        )
                        color = Colors.ERROR.color
                    }
                }
            } catch (e: Exception) {
                message.error("I couldn't DM that user the ban reason, they might have had DMs disabled.")
            }
        }

        try {
            user.ban(
                server,
                deleteMessageDays,
                reason
            )
            if (sendReasonFeedback) {
                message.channel.send {
                    embed {
                        field(
                            "${user.name}#${user.discriminator} was banned by:",
                            message.author!!.mention,
                            false
                        )
                        field(
                            banReason,
                            reason ?: "None Provided",
                            false
                        )
                        footer("ID: ${user.id}", user.avatar.url)
                        color = Colors.ERROR.color
                    }
                }
            }
        } catch (e: Exception) {
            message.channel.send {
                embed {
                    title = "That user's role is higher then mine, I can't ban them!"
                    field("Stacktrace:", "```$e```")
                    color = Colors.ERROR.color
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "$fullName <user(id, username, ping)>\n" +
                "$fullName <user(id, username, ping)> <reason>\n" +
                "$fullName <user(id, username, ping)> <delete messages (boolean)> <reason>"
    }
}