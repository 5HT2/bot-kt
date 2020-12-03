package org.kamiblue.botkt.commands

import org.kamiblue.botkt.*
import org.kamiblue.botkt.PermissionTypes.MANAGE_CHANNELS
import org.kamiblue.botkt.utils.MessageSendUtils.normal
import org.kamiblue.botkt.utils.MessageSendUtils.success
import org.kamiblue.botkt.utils.StringUtils.flat

object TopicCommand : Command("topic") {
    init {
        literal("set") {
            greedyString("topic") {
                doesLaterIfHas(MANAGE_CHANNELS) { context ->
                    var setTopic: String = context arg "topic"
                    setTopic = setTopic.flat(1024)

                    message.serverChannel?.edit {
                        topic = setTopic
                    }

                    message.success("Set Channel Topic to `$setTopic`!")
                }
            }

        }

        literal("clear") {
            doesLaterIfHas(MANAGE_CHANNELS) {
                message.serverChannel?.edit {
                    topic = null
                }

                message.success("Cleared Channel Topic!")
            }
        }

        doesLater {
            val topic = message.serverChannel?.topic
            message.normal(if (topic.isNullOrEmpty()) "No topic set!" else topic, "#" + message.serverChannel?.name)
        }
    }
}