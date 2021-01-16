@file:Suppress("UNUSED")

package org.kamiblue.botkt.utils

import com.google.gson.JsonObject
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import net.ayataka.kordis.DiscordClientImpl
import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.deleteAll
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.message.MessageImpl
import net.ayataka.kordis.entity.message.embed.EmbedBuilder
import net.ayataka.kordis.entity.server.channel.ServerChannel
import net.ayataka.kordis.exception.NotFoundException
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.utils.StringUtils.elseEmpty
import org.kamiblue.botkt.utils.StringUtils.joinToChunks
import java.io.File

fun <E> EmbedBuilder.joinToFields(
    iterable: Iterable<E>,
    separator: CharSequence = ", ",
    titlePrefix: String = "",
    lineTransformer: (E) -> String = { it.toString() }
) {
    val chunks = iterable.joinToChunks(separator, 1024, lineTransformer)

    for ((index, chunk) in chunks.withIndex()) {
        field("$titlePrefix ${index + 1} / ${chunks.size}", chunk.elseEmpty("Empty"))
    }
}

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

suspend fun TextChannel.upload(files: Collection<File>, message: String = ""): Message = when {
    files.isEmpty() -> {
        throw IllegalArgumentException("files can not be empty!")
    }
    files.size > 10 -> {
        throw IllegalArgumentException("Exceeded attachment limit: ${files.size}/10")
    }
    else -> {
        Main.discordHttp.post<JsonObject> {
            url("https://discord.com/api/v8/channels/$id/messages")
            header("Accept", ContentType.MultiPart.FormData)
            body = MultiPartFormDataContent(
                formData {
                    if (message.isNotBlank()) append("content", message)
                    files.forEach { appendFile(it) }
                }
            )
        }.toMessage(this)
    }
}

suspend fun TextChannel.upload(file: File, message: String = "", embed: JsonObject? = null): Message = Main.discordHttp.post<JsonObject> {
    url("https://discord.com/api/v8/channels/$id/messages")
    header("Accept", ContentType.MultiPart.FormData)
    body = MultiPartFormDataContent(
        formData {
            if (embed != null) append("payload_json", embed.toString())
            if (message.isNotBlank()) append("content", message) // this will not be added if embed isn't null
            appendFile(file)
        }
    )
}.toMessage(this)

suspend fun Message.safeDelete() {
    try {
        this.delete()
    } catch (e: NotFoundException) {
        Main.logger.debug("Failed to delete ${this}\n${e.stackTraceToString()}")
    }
}

suspend fun Collection<Message>.safeDelete() {
    try {
        this.deleteAll()
    } catch (e: NotFoundException) {
        Main.logger.debug("Failed to delete ${this}\n${e.stackTraceToString()}")
    }
}

val Message.link get() = "https://discord.com/channels/${this.server?.id}/${this.channel.id}/${this.id}"

private fun FormBuilder.appendFile(file: File) = appendInput(
    key = file.absolutePath,
    headers = Headers.build { append(HttpHeaders.ContentDisposition, "filename=${file.name}") },
    size = file.length(),
    block = {
        buildPacket { writeFully(file.readBytes()) }
    }
)

private fun JsonObject.toMessage(channel: TextChannel) =
    MessageImpl(Main.client as DiscordClientImpl, this, (channel as? ServerChannel?)?.server)
