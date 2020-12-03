package org.kamiblue.botkt.utils

import net.ayataka.kordis.entity.message.Message

@Suppress("UNUSED")
object MessageSendUtils {
    fun log(message: String, prefix: String = "[bot-kt]") = println("$prefix $message")

    suspend fun Message.normal(description: String, title: String) = channel.send {
        embed {
            this.title = title
            this.description = description
            this.color = Colors.PRIMARY.color
        }
    }

    suspend fun Message.normal(description: String) = channel.send {
        embed {
            this.description = description
            this.color = Colors.PRIMARY.color
        }

    }

    suspend fun Message.success(description: String) = channel.send {
        embed {
            this.description = description
            color = Colors.SUCCESS.color
        }
    }


    suspend fun Message.error(description: String) = channel.send {
        embed {
            this.title = "Error"
            this.description = description
            this.color = Colors.ERROR.color
        }
    }


    suspend fun Message.stackTrace(e: Exception) = channel.send {
        embed {
            title = "Error"
            description = "```" + e.message + "```\n```" + e.stackTrace.joinToString("\n") + "```"
            color = Colors.ERROR.color
        }
    }

    suspend fun Message.warn(description: String) {
        channel.send {
            embed {
                this.title = "Warning"
                this.description = description
                this.color = Colors.WARN.color
            }
        }
    }
}