package org.kamiblue.botkt.command.commands.moderation

import kotlinx.coroutines.delay
import net.ayataka.kordis.entity.deleteAll
import net.ayataka.kordis.entity.message.Message
import org.kamiblue.botkt.PermissionTypes.COUNCIL_MEMBER
import org.kamiblue.botkt.PermissionTypes.PURGE_PROTECTED
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageUtils.error
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable

object PurgeCommand : BotCommand(
    name = "purge",
    category = Category.MODERATION,
    description = "Purges a number of messages in a channel based on parameters."
) {
    init {
        int("amount") { numberArg ->
            executeIfHas(COUNCIL_MEMBER, "Purge X messages, excluding protected") {
                val msgs = message.channel
                    .getMessages()
                    .filter { !it.author.hasPermission(COUNCIL_MEMBER) && it.author?.bot == false }
                    .take(numberArg.value)

                purge(msgs, message)
            }

            boolean("delete protected msgs") { protected ->
                executeIfHas(PURGE_PROTECTED, "Purge X messages, including council & bot") {
                    val msgs = message.channel
                        .getMessages()
                        .filter { protected.value || !it.author.hasPermission(COUNCIL_MEMBER) && it.author?.bot == false }
                        .take(numberArg.value)

                    purge(msgs, message)
                }
            }

            user("purge this user") { userArg ->
                executeIfHas(COUNCIL_MEMBER, "Purge X messages sent by a user") {
                    val user = userArg.value
                    if (!message.author.hasPermission(PURGE_PROTECTED) && user.hasPermission(COUNCIL_MEMBER) || user.bot) {
                        channel.error(
                            "Sorry, but you're missing the " +
                                "'${PURGE_PROTECTED.name.toHumanReadable()}'" +
                                " permission, which is required to purge " +
                                "'${COUNCIL_MEMBER.name.toHumanReadable()}'" +
                                " messages / bot messages"
                        )
                        return@executeIfHas
                    }

                    val msgs = message.channel
                        .getMessages()
                        .filter { it.author?.id == user.id }
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

        msgs.deleteAll()
        delay(5000)
        response.delete()
        message.delete()
    }
}
