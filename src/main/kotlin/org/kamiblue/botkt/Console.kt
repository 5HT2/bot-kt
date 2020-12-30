package org.kamiblue.botkt

import net.ayataka.kordis.DiscordClient
import net.ayataka.kordis.entity.channel.TextChannel
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.message.MessageBuilder
import net.ayataka.kordis.entity.message.MessageType
import net.ayataka.kordis.entity.message.attachment.Attachment
import net.ayataka.kordis.entity.message.embed.Embed
import net.ayataka.kordis.entity.message.embed.EmbedBuilder
import net.ayataka.kordis.entity.message.embed.EmbedImpl
import net.ayataka.kordis.entity.message.embed.Field
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import net.ayataka.kordis.utils.asStringOrNull
import net.ayataka.kordis.utils.getArrayOrNull
import net.ayataka.kordis.utils.getOrNull
import org.kamiblue.botkt.command.CommandManager
import java.time.Instant

internal object Console {

    fun start() {
        while (true) {
            val line = readLine()
            if (line.isNullOrBlank()) continue

            val event = MessageReceiveEvent(FakeMessage(line))
            CommandManager.runCommand(event, line.removePrefix(Main.prefix.toString()))
        }
    }

    class FakeMessage(
        override val content: String,
        override val embeds: Collection<Embed> = emptyList()
    ) : Message {
        override val attachments: Collection<Attachment> = emptyList()
        override val author: User? = null
        override val channel: TextChannel = ConsoleChannel
        override val client: DiscordClient = Main.client
        override val editedTimestamp: Instant? = null
        override val id: Long = (System.currentTimeMillis() - 1420070400000L) shl 22
        override val mentionsEveryone: Boolean = false
        override val pinned: Boolean = false
        override val server: Server? = null
        override val tts: Boolean = false
        override val type: MessageType = MessageType.UNKNOWN

        override suspend fun edit(text: String, embed: (EmbedBuilder.() -> Unit)?): Message {
            return channel.send {
                content = text
                if (embed != null) {
                    embed {
                        embed()
                    }
                }
            }
        }

        override fun toString(): String {
            return StringBuilder().run {
                if (content.isNotBlank()) appendLine(content)
                if (embeds.isNotEmpty()) {
                    appendLine("Embed:")
                    embeds.forEach {
                        appendLine(it.asString())
                    }
                }
                toString()
            }
        }

        private fun Embed.asString(): String {
            return StringBuilder().run {
                if (title != null) appendLine("Title: $title")
                if (fields.isNotEmpty()) {
                    appendLine("Fields")
                    fields.forEach {
                        appendLine("${it.name}\n${it.value}")
                    }
                }
                if (description != null) appendLine("Description:\n$description")
                toString()
            }
        }
    }

    private object ConsoleChannel : TextChannel {
        override val client: DiscordClient = Main.client
        override val id: Long = 0x22

        override suspend fun deleteMessage(messageId: Long) {
            return
        }

        override suspend fun getMessage(messageId: Long): Message? {
            return null
        }

        override suspend fun getMessages(limit: Int): Collection<Message> {
            return emptyList()
        }

        override suspend fun send(text: String): Message {
            return FakeMessage(text).also {
                println(it.toString())
            }
        }

        override suspend fun send(block: MessageBuilder.() -> Unit): Message {
            return MessageBuilder().run {
                block()

                val embeds = embed?.let { jsonObject ->
                    listOf(
                        EmbedImpl(
                            title = jsonObject.getOrNull("title")?.asStringOrNull,
                            description = jsonObject.getOrNull("description")?.asStringOrNull,
                            url = null,
                            color = null,
                            timestamp = null,
                            imageUrl = null,
                            thumbnailUrl = null,
                            footer = null,
                            author = null,
                            fields = jsonObject.getArrayOrNull("fields")?.map {
                                Field(
                                    it.asJsonObject["name"].asString,
                                    it.asJsonObject["value"].asString,
                                    it.asJsonObject.getOrNull("inline")?.asBoolean == true
                                )
                            } ?: emptyList()
                        )
                    )
                } ?: emptyList()

                FakeMessage(content, embeds).also {
                    println(it.toString())
                }
            }
        }
    }
}
