package commands

import Colors
import Command
import FakeUser
import arg
import authenticatedRequest
import doesLater
import getAuthToken
import greedyString
import helpers.StringHelper.toHumanReadable
import net.ayataka.kordis.entity.findByTag

object WhoisCommand : Command("whois") {
    init {
        greedyString("name") {
            doesLater { context ->
                val username: String = context arg "name"
                val member: net.ayataka.kordis.entity.server.member.Member? = try {
                    message.server?.members?.find(username.toLong())
                } catch (e: NumberFormatException) {
                    if (message.server?.members?.findByTag(username) == null) {
                        message.server?.members?.findByName(username)
                    } else {
                        message.server?.members?.findByTag(username)
                    }
                }
                if (member == null) {
                    val fetchUser = authenticatedRequest<FakeUser>(
                        "Bot",
                        getAuthToken(),
                        "https://discord.com/api/v6/users/${member?.id}"
                    )
                    val discriminator = when (fetchUser.discriminator.toString().length) {
                        1 -> "${fetchUser.discriminator}000"
                        2 -> "${fetchUser.discriminator}00"
                        3 -> "${fetchUser.discriminator}0"
                        else -> fetchUser.discriminator.toString()
                    }
                    message.channel.send {
                        embed {
                            title = "${fetchUser.username}#$discriminator"
                            color = Colors.primary
                            thumbnailUrl = "https://cdn.discordapp.com/avatars/${fetchUser.id}/${fetchUser.avatar}.png"
                        }
                    }
                } else {
                    message.channel.send {
                        embed {
                            title = member.tag
                            color = Colors.primary
                            thumbnailUrl = member.avatar.url
                            field("Created:", "Placeholder", true) /* TODO: Create time for discord*/
                            field("Joined:", member.joinedAt.toString(), true)
                            field("Name:", member.nickname ?: member.name)
                            field("Mention:", member.mention)
                            field("Online:", member.status.toString().toHumanReadable())
                            field("Playing:", "Placeholder") /* TODO: Show custom status */
                        }
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "$fullName + <user id/user tag/user name>"
    }
}