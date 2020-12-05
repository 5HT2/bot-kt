package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.utils.MessageSendUtils.normal
import org.kamiblue.botkt.utils.MessageSendUtils.success
import org.kamiblue.botkt.utils.StringUtils.flat

object TopicCommand : BotCommand(
    "topic"
) {
    init {
        literal("set") {
            greedy("topic") { topicArg ->
                executeIfHas(PermissionTypes.MANAGE_CHANNELS) {
                    val setTopic = topicArg.value.flat(1024)

                    message.serverChannel?.edit {
                        topic = setTopic
                    }

                    message.success("Set Channel Topic to `$setTopic`!")
                }
            }

        }

        literal("clear") {
            executeIfHas(PermissionTypes.MANAGE_CHANNELS) {

                message.serverChannel?.edit {
                    topic = null
                }

                message.success("Cleared Channel Topic!")
            }
        }

        execute {
            val topic = message.serverChannel?.topic
            message.normal(if (topic.isNullOrEmpty()) "No topic set!" else topic, "#" + message.serverChannel?.name)
        }
    }
}