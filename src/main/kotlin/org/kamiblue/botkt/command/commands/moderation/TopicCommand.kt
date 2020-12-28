package org.kamiblue.botkt.command.commands.moderation

import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.utils.normal
import org.kamiblue.botkt.utils.success
import org.kamiblue.commons.extension.max

object TopicCommand : BotCommand(
    name = "topic",
    category = Category.MODERATION,
    description = "View and edit the channel topic"
) {
    init {
        literal("set") {
            greedy("topic") { topicArg ->
                executeIfHas(PermissionTypes.MANAGE_CHANNELS, "Set the topic of the channel") {
                    val setTopic = topicArg.value.max(1024)

                    message.serverChannel?.edit {
                        topic = setTopic
                    }

                    channel.success("Set Channel Topic to `$setTopic`!")
                }
            }
        }

        literal("clear") {
            executeIfHas(PermissionTypes.MANAGE_CHANNELS, "Reset the topic of the channel to nothing") {

                message.serverChannel?.edit {
                    topic = null
                }

                channel.success("Cleared Channel Topic!")
            }
        }

        execute("Print the topic of the channel") {
            val topic = message.serverChannel?.topic
            channel.normal(
                if (topic.isNullOrEmpty()) "No topic set!" else topic,
                "#" + message.serverChannel?.name
            )
        }
    }
}
