package org.kamiblue.botkt.commands

import org.kamiblue.botkt.Command
import org.kamiblue.botkt.doesLater
import org.kamiblue.botkt.utils.Colors

object PingCommand : Command("ping") {
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
