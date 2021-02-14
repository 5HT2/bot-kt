package org.kamiblue.botkt.command

import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.Console
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.Permissions.missingPermissions
import org.kamiblue.botkt.entity.Emoji
import org.kamiblue.command.CommandBuilder
import org.kamiblue.command.args.AbstractArg
import org.kamiblue.command.utils.BuilderBlock
import org.kamiblue.command.utils.ExecuteBlock

abstract class BotCommand(
    name: String,
    alias: Array<out String> = emptyArray(),
    val category: Category,
    description: String = "No description",
) : CommandBuilder<MessageExecuteEvent>(name, alias, description) {

    @CommandBuilder
    protected fun AbstractArg<*>.executeIfHas(
        permission: PermissionTypes,
        description: String = "No description",
        block: ExecuteBlock<MessageExecuteEvent>
    ) {
        val blockWithIf: ExecuteBlock<MessageExecuteEvent> = {
            if (this.message is Console.FakeMessage || this.message.author.hasPermission(permission)) {
                block.invoke(this)
            } else {
                this.message.missingPermissions(permission)
            }
        }
        this.execute(description, block = blockWithIf)
    }

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
