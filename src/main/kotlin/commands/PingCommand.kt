package commands

import Colors
import Command
import doesLater

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
