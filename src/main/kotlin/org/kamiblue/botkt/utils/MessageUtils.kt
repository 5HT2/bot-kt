package org.kamiblue.botkt.utils

import net.ayataka.kordis.entity.channel.TextChannel

@Suppress("UNUSED")
object MessageUtils {

    suspend fun TextChannel.normal(description: String, title: String? = null) = send {
        embed {
            this.title = title
            this.description = description
            color = Colors.PRIMARY.color
        }
    }

    suspend fun TextChannel.success(description: String, title: String? = null) = send {
        embed {
            this.title = title
            this.description = description
            color = Colors.SUCCESS.color
        }
    }

    suspend fun TextChannel.warn(description: String) = send {
        embed {
            this.title = "Warning"
            this.description = description
            this.color = Colors.WARN.color
        }
    }

    suspend fun TextChannel.error(description: String) = send {
        embed {
            this.title = "Error"
            this.description = description
            this.color = Colors.ERROR.color
        }
    }

    suspend fun TextChannel.stackTrace(e: Exception) = send {
        embed {
            title = "Error"
            description = "```" + e.message + "```\n```" + e.stackTrace.joinToString("\n") + "```"
            color = Colors.ERROR.color
        }
    }
}