package org.kamiblue.botkt.commands

import net.ayataka.kordis.entity.server.channel.ServerChannel
import org.kamiblue.botkt.*
import org.kamiblue.botkt.utils.MessageSendUtils.normal

object SayCommand : Command("say") {
    init {
        channel("channel") {
            doesLaterIfHas(PermissionTypes.ANNOUNCE) { context ->
                val channel: ServerChannel? = context.channelArg("channel", server)

                message.normal(channel.toString())
            }
        }
    }
}