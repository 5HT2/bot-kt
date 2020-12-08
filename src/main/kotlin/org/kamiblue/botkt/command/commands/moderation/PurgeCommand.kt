package org.kamiblue.botkt.command.commands.moderation

import net.ayataka.kordis.entity.deleteAll
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category

object PurgeCommand : BotCommand(
    name = "purge",
    category = Category.MODERATION,
    description = "Purges a number of messages in a channel based on parameters."
) {
    init {
        int("amount") { numberArg ->
            executeIfHas(PermissionTypes.COUNCIL_MEMBER) {
                val number = numberArg.value + 1
                message.channel.getMessages(number).deleteAll()
            }

            greedy("user") { userArg ->
                executeIfHas(PermissionTypes.COUNCIL_MEMBER) {
                    val number = numberArg.value + 1
                    val user = userArg.value

                    val search = if (number < 1000) number * 2 + 50 else number
                    message.channel.getMessages(search)
                        .filter {
                            it.author?.id.toString() == user
                                || it.author?.mention == user
                                || it.author?.tag == user
                        }
                        .take(number).deleteAll()
                }
            }
        }
    }
}
