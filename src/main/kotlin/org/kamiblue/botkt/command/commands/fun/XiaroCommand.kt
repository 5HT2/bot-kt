package org.kamiblue.botkt.command.commands.`fun`

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category

object XiaroCommand : BotCommand(
    name = "xiaro",
    alias = arrayOf("popbob", "transrights"),
    category = Category.FUN,
    description = "Prepend `Trans rights~! :3` to your message."
) {
    private val pingRegex = "<@.*>|@everyone|@here".toRegex()
    private const val transRightsWithComma = "Trans rights~! :3, "
    private const val transRights = "Trans rights~! :3"

    init {
        greedy("message") { messageArg ->
            execute {
                val messageInput = messageArg.value
                if (!messageInput.contains(pingRegex)) {
                    message.channel.send(transRightsWithComma + messageInput)
                }
            }
        }

        execute {
            message.channel.send(transRights)
        }
    }
}