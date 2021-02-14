package org.kamiblue.botkt.command.commands.moderation

import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.options.HasPermission
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.error
import org.kamiblue.botkt.utils.findUserEverywhere

object UnbanCommand : BotCommand(
    name = "unban",
    category = Category.MODERATION,
    description = "Unban a user"
) {
    init {
        greedy("name") { nameArg ->
            execute(HasPermission.get(PermissionTypes.COUNCIL_MEMBER)) {
                val server = server ?: run {
                    channel.error("This command must be called from a server!")
                    return@execute
                }

                val name = nameArg.value
                val user = findUserEverywhere(name) ?: return@execute

                server.unban(user)

                channel.send {
                    embed {
                        field(
                            "${user.tag} was unbanned by:",
                            message.author?.mention ?: "Unban message author not found!"
                        )
                        footer("ID: ${user.id}", user.avatar.url)
                        color = Colors.SUCCESS.color
                    }
                }
            }
        }
    }
}
