package org.kamiblue.botkt.command.commands.moderation

import net.ayataka.kordis.entity.find
import org.kamiblue.botkt.MuteManager
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
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
            executeIfHas(PermissionTypes.COUNCIL_MEMBER) {
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
                        serverMuteInfo.coroutineMap.remove(member.id)?.cancel()
                        member.removeRole(serverMuteInfo.getMutedRole())
                        message.success("${member.name}#${member.discriminator} is unmuted")
                    } catch (e: Exception) {
                        message.channel.send {
                            embed {
                                title = "Unable to unmute ${member.mention}"
                                description = e.message
                                color = Colors.ERROR.color
                            }
                        }
                    }
                } else {
                    message.error("${member.name}#${member.discriminator} is not muted")
                }
            }
        }
    }

}