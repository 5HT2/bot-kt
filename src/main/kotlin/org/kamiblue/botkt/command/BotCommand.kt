package org.kamiblue.botkt.command

import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.entity.Emoji
import org.kamiblue.command.CommandBuilder
import org.kamiblue.command.args.AbstractArg
import org.kamiblue.command.utils.BuilderBlock

abstract class BotCommand(
    name: String,
    alias: Array<out String> = emptyArray(),
    val category: Category,
    description: String = "No description",
) : CommandBuilder<MessageExecuteEvent>(name, alias, description) {

    @CommandBuilder
    protected fun AbstractArg<*>.channel(
        name: String,
        block: BuilderBlock<Long>
    ) {
        arg(ChannelArg(name), block)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.emoji(
        name: String,
        block: BuilderBlock<Emoji>
    ) {
        arg(EmojiArg(name), block)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.user(
        name: String,
        block: BuilderBlock<User>
    ) {
        arg(UserArg(name), block)
    }
}
