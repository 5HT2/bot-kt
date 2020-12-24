@file:Suppress("UNUSED")

package org.kamiblue.botkt.utils

import com.google.gson.JsonObject
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import net.ayataka.kordis.DiscordClientImpl
import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.message.MessageImpl
import net.ayataka.kordis.entity.server.channel.ServerChannel
import org.kamiblue.botkt.Main
import java.io.File

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

suspend fun TextChannel.upload(file: File) : Message = Main.discordHttp.post<JsonObject> {
    url("https://discord.com/api/v8/channels/${id}/messages")
    header("Accept", ContentType.MultiPart.FormData)
    body = MultiPartFormDataContent(
        formData {
            appendInput(
                key = file.absolutePath,
                headers = Headers.build { append(HttpHeaders.ContentDisposition, "filename=${file.name}") },
                size = file.length(),
                block = {
                    buildPacket { writeFully(file.readBytes()) }
                }
            )
        }
    )
}.let {
    MessageImpl(Main.client as DiscordClientImpl, it, (this as? ServerChannel?)?.server)
}