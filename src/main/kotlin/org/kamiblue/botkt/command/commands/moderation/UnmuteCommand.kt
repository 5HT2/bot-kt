package org.kamiblue.botkt.command.commands.moderation

import net.ayataka.kordis.entity.find
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.MuteManager
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.commands.system.ExceptionCommand
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.success

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

                if (serverMuteInfo.muteMap.remove(member.id) != null) {
                    try {
                        member.getPrivateChannel().send {
                            embed {
                                field(
                                    "You were unmuted by:",
                                    "${message.author?.mention ?: "Mute message author not found!"}, in the guild `${server.name}`"
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

                    try {
                        serverMuteInfo.coroutineMap.remove(member.id)?.cancel()
                        member.removeRole(serverMuteInfo.getMutedRole())
                        message.channel.send {
                            embed {
                                field(
                                    "${member.name}#${member.discriminator} was unmute by:",
                                    message.author?.mention ?: "Mute message author not found!"
                                )
                                footer("ID: ${member.id}", member.avatar.url)
                                color = Colors.SUCCESS.color
                            }
                        }
                    } catch (e: Exception) {
                        ExceptionCommand.addException(e)
                        message.channel.send {
                            embed {
                                title = "Unable to unmute ${member.mention}"
                                description = "${e.message}\n" +
                                    "Use the `${Main.prefix}exception` command to view the full stacktrace."
                                color = Colors.ERROR.color
                            }
                        }
                    }
                } else {
                    message.error("${member.mention} is not muted")
                }
            }
        }
    }

}