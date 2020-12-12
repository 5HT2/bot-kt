package org.kamiblue.botkt.command.commands.moderation

import net.ayataka.kordis.entity.find
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.MuteManager
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error

object UnmuteCommand : BotCommand(
    name = "unmute",
    alias = arrayOf("unshut"),
    category = Category.MODERATION,
    description = "Now start talking!"
) {
    init {
        user("user") { userArg ->
            executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Unmute user") {
                if (server == null) {
                    message.error("Server is null, are you running this from a DM?")
                    return@executeIfHas
                }

                val member = server.members.find(userArg.value) ?: run {
                    message.error("Member not found!")
                    return@executeIfHas
                }

                val serverMuteInfo = MuteManager.serverMap.getOrPut(server.id) { MuteManager.ServerMuteInfo(server) }

                when {
                    serverMuteInfo.muteMap.remove(member.id) != null -> {
                        sendUnMute(member)

                        serverMuteInfo.coroutineMap.remove(member.id)?.cancel()
                        member.removeRole(serverMuteInfo.getMutedRole())
                        message.channel.send {
                            embed {
                                field(
                                    "${member.tag} was unmuted by:",
                                    message.author?.mention.toString()
                                )
                                footer("ID: ${member.id}", member.avatar.url)
                                color = Colors.SUCCESS.color
                            }
                        }
                    }

                    member.roles.contains(server.roles.findByName("Muted", true)) -> { // todo no work
                        sendUnMute(member)

                        message.channel.send {
                            embed {
                                description = "Warning: ${member.mention} was not muted using the bot, removed muted role."
                                field(
                                    "${member.tag} was unmuted by:",
                                    message.author?.mention.toString()
                                )
                                footer("ID: ${member.id}", member.avatar.url)
                                color = Colors.WARN.color
                            }
                        }
                    }

                    else -> {
                        message.error("${member.mention} is not muted")
                    }
                }
            }
        }
    }

    private suspend fun MessageExecuteEvent.sendUnMute(member: Member) {
        try {
            member.getPrivateChannel().send {
                embed {
                    field(
                        "You were unmuted by:",
                        message.author?.mention.toString()
                    )
                    field(
                        "In the guild:",
                        server?.name.toString()
                    )
                    color = Colors.SUCCESS.color
                    footer("ID: ${message.author?.id}", message.author?.avatar?.url)
                }
            }
        } catch (e: Exception) {
            message.channel.send {
                embed {
                    title = "Error"
                    description = "I couldn't DM that user the unmute, they might have had DMs disabled."
                    color = Colors.ERROR.color
                }
            }
        }
    }

}