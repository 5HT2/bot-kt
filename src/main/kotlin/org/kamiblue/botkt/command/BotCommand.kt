package org.kamiblue.botkt.command

import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.Permissions.missingPermissions
import org.kamiblue.botkt.command.arguments.DiscordChannelArg
import org.kamiblue.botkt.command.arguments.DiscordEmojiArg
import org.kamiblue.botkt.command.arguments.DiscordUserArg
import org.kamiblue.botkt.utils.AnimatableEmoji
import org.kamiblue.command.AbstractArg
import org.kamiblue.command.CommandBuilder
import org.kamiblue.command.utils.BuilderBlock
import org.kamiblue.command.utils.ExecuteBlock

abstract class BotCommand(
    name: String,
    alias: Array<out String> = emptyArray(),
    val category: Category ,
    description: String = "No description",
) : CommandBuilder<MessageExecuteEvent>(name, alias, description) {

    @CommandBuilder
    protected fun AbstractArg<*>.executeIfHas(
        permission: PermissionTypes,
        description: String = "No description",
        block: ExecuteBlock<MessageExecuteEvent>
    ) {
        val blockWithIf: ExecuteBlock<MessageExecuteEvent> = {
            if (this.message.author!!.id.hasPermission(permission)) {
                block.invoke(this)
            } else {
                this.message.missingPermissions(permission)
            }
        }
        this.execute(description, blockWithIf)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.channel(
        name: String,
        block: BuilderBlock<Long>
    ) {
        arg(DiscordChannelArg(name), block)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.emoji(
        name: String,
        block: BuilderBlock<AnimatableEmoji>
    ) {
        arg(DiscordEmojiArg(name), block)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.user(
        name: String,
        block: BuilderBlock<User>
    ) {
        arg(DiscordUserArg(name), block)
    }

}