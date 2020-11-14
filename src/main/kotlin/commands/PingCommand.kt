package org.kamiblue.botkt.commands

import org.kamiblue.botkt.Colors
import org.kamiblue.botkt.Command
import org.kamiblue.botkt.doesLater

object PingCommand : Command("ping") {
    init {
        doesLater {
            val m = message.channel.send {
                embed {
                    description = "Ping?"
                    color = Colors.primary
                }
            }

            m.edit {
                description = "Pong! ${System.currentTimeMillis() - m.timestamp.toEpochMilli()}ms."
                color = Colors.success
            }

        }

    }
}
