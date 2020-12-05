package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.utils.Colors

object PingCommand : BotCommand(
    name = "ping",
    description = "Pong!"
) {
    init {
        execute {
            val message = message.channel.send {
                embed {
                    description = "Ping?"
                    color = Colors.PRIMARY.color
                }
            }

            message.edit {
                description = "Pong! ${System.currentTimeMillis() - message.timestamp.toEpochMilli()}ms."
                color = Colors.SUCCESS.color
            }

        }

    }
}
