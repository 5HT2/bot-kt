package commands

import Colors
import Command
import arg
import doesLater
import ping
import userArg

object TestCommand : Command("t") {
    init {
        ping("ping") {
            doesLater { context ->
                val user = context userArg "ping" ?: error("You didn't specify a valid user.")
                val uwu: String = context arg "uwu"

                message.channel.send {
                    content = uwu
                    embed {
                        color = Colors.primary
                        imageUrl = user.avatar.url
                    }
                }
            }
        }
    }
}