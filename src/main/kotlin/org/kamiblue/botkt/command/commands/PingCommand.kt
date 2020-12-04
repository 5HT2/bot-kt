package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.command.CommandOld
import org.kamiblue.botkt.command.doesLater
import org.kamiblue.botkt.utils.Colors

object PingCommand : CommandOld("ping") {
    init {
        doesLater {
            val m = message.channel.send {
                embed {
                    description = "Ping?"
                    color = Colors.PRIMARY.color
                }
            }

            m.edit {
                description = "Pong! ${System.currentTimeMillis() - m.timestamp.toEpochMilli()}ms."
                color = Colors.SUCCESS.color
            }

        }

    }
}
