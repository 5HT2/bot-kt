package org.kamiblue.botkt.command.commands.moderation

import kotlinx.coroutines.delay
import net.ayataka.kordis.entity.message.Message
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.options.HasPermission
import org.kamiblue.botkt.utils.*
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable

object PurgeCommand : BotCommand(
    name = "purge",
    category = Category.MODERATION,
    description = "Purges a number of messages in a channel based on parameters."
) {
    init {
        int("amount") { numberArg ->
            execute("Purge X messages, excluding protected", HasPermission.get(PermissionTypes.COUNCIL_MEMBER)) {
                val msgs = message.channel
                    .getMessages()
                    .filter { !it.author.hasPermission(PermissionTypes.COUNCIL_MEMBER) && it.author?.bot == false }
                    .take(numberArg.value)

                purge(msgs, message)
            }
        }

        user("purge this user") { userArg ->
            int("amount") { numberArg ->
                execute("Purge X messages sent by a user", HasPermission.get(PermissionTypes.COUNCIL_MEMBER)) {
                    val user = userArg.value
                    if (
                        message.author?.id != user.id &&
                        !message.author.hasPermission(PermissionTypes.PURGE_PROTECTED) &&
                        (user.hasPermission(PermissionTypes.COUNCIL_MEMBER) || user.bot)
                    ) {
                        channel.error(
                            "Sorry, but you're missing the " +
                                "'${PermissionTypes.PURGE_PROTECTED.name.toHumanReadable()}'" +
                                " permission, which is required to purge " +
                                "'${PermissionTypes.COUNCIL_MEMBER.name.toHumanReadable()}'" +
                                " messages / bot messages"
                        )
                        return@execute
                    }

                    val msgs = message.channel
                        .getMessages()
                        .filter { it.author?.id == user.id }
                        .take(numberArg.value)

                    purge(msgs, message)
                }
            }
        }

        literal("protected", "force") {
            int("amount") { numberArg ->
                execute("Purge X messages, including council & bot", HasPermission.get(PermissionTypes.PURGE_PROTECTED)) {
                    val msgs = message.channel
                        .getMessages()
                        .take(numberArg.value)

                    purge(msgs, message)
                }
            }
        }
    }

    private suspend fun purge(msgs: Collection<Message>, message: Message) {
        val response = message.channel.send {
            embed {
                field(
                    "${msgs.size} messages were purged by:",
                    message.author?.mention.toString()
                )
                footer("ID: ${message.author?.id}", message.author?.avatar?.url)
                color = Colors.ERROR.color
            }
        }

        msgs.tryDeleteAll() // we want to safe delete because messages could get deleted by users while purging
        delay(5000)
        response.tryDelete()
        message.tryDelete()
    }
}
