package org.kamiblue.botkt.command.commands.system

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.formatDuration
import org.kamiblue.botkt.utils.untilNow
import java.time.Instant
import java.time.temporal.ChronoUnit

object UptimeCommand : BotCommand(
    name = "uptime",
    category = Category.SYSTEM,
    description = "Uptime of the bot"
) {
    init {
        execute {
            val uptimeMillis = Instant.ofEpochSecond(1605034200).untilNow(ChronoUnit.MILLIS)
            message.channel.send {
                embed {
                    title = "Uptime"
                    description = formatDuration(uptimeMillis)
                    color = Colors.PRIMARY.color
                }
            }
        }
    }
}
