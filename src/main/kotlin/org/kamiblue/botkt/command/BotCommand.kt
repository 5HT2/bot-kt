package org.kamiblue.botkt.command

import org.kamiblue.command.CommandBuilder

abstract class BotCommand(
    name: String,
    alias: Array<out String> = emptyArray(),
    description: String = "",
) : CommandBuilder<MessageExecuteEvent>(name, alias, description)