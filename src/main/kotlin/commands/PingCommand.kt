package commands

import Command
import Main.Colors.BLUE
import Main.Colors.SUCCESS
import doesLater

object PingCommand : Command("ping") {
    init {
        doesLater {
            val m = message.channel.send {
                embed {
                    description = "Ping?"
                    color = BLUE.color
                }
            }

            m.edit {
                description = "Pong! ${System.currentTimeMillis() - m.timestamp.toEpochMilli()}ms."
                color = SUCCESS.color
            }

        }

    }
}
