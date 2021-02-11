package org.kamiblue.botkt.manager.managers

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.event.events.message.MessageDeleteEvent
import net.ayataka.kordis.event.events.message.MessageEditEvent
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.config.ServerConfigs
import org.kamiblue.botkt.config.server.LoggingConfig
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.contextLink
import org.kamiblue.botkt.utils.joinToFields
import org.kamiblue.botkt.utils.prettyFormat
import org.kamiblue.event.listener.asyncListener
import java.time.Instant

object LoggingManager {
    private val mutex = Mutex()
    private val cachedMessages = LinkedHashMap<Long, Message>(200)

    init {
        asyncListener<MessageEditEvent> {
            val server = it.server ?: return@asyncListener
            val message = it.message ?: return@asyncListener
            val oldMessage = mutex.withLock {
                cachedMessages[it.messageId]
            }

            log(server, oldMessage, message, true)

            cacheMessage(message)
        }

        asyncListener<MessageDeleteEvent> { event ->
            val server = event.server ?: return@asyncListener

            val messages = mutex.withLock {
                event.messageIds.mapNotNull {
                    cachedMessages.remove(it)
                }
            }

            messages.sortedBy {
                it.timestamp.prettyFormat()
            }.forEach {
                log(server, null, it, false)
                delay(100)
            }
        }

        asyncListener<MessageReceiveEvent> {
            cacheMessage(it.message)
        }
    }


    private suspend fun log(server: Server, oldMessage: Message?, message: Message, edit: Boolean) {
        val config = ServerConfigs.get<LoggingConfig>(server)

        if (config.loggingChannel == -1L) return
        if (config.loggingChannel == message.channel.id) return
        if (config.ignoredChannels.contains(message.channel.id)) return

        val loggingChannel = server.textChannels.find(config.loggingChannel) ?: return

        if (config.ignoredPrefix.isNotBlank()) {
            if (message.author.hasPermission(PermissionTypes.COUNCIL_MEMBER) &&
                (message.content.startsWith(config.ignoredPrefix) || oldMessage?.content?.startsWith(config.ignoredPrefix) == true)
            ) return
        }

        if (edit) {
            loggingChannel.send {
                embed {
                    oldMessage?.let {
                        joinToFields(it.content.lines(), "\n", titlePrefix = "Original Message")
                    }
                    joinToFields(message.content.lines(), "\n", titlePrefix = "New Message")

                    description = message.contextLink
                    author(message.author?.tag, iconUrl = message.author?.avatar?.url)
                    footer("ID: ${message.author?.id}")
                    timestamp = Instant.now()
                    color = Colors.EDITED_MESSAGE.color
                }
            }
        } else {
            loggingChannel.send {
                embed {
                    joinToFields(message.content.lines(), "\n", titlePrefix = "Deleted Message")
                    description = message.contextLink
                    author(message.author?.tag, iconUrl = message.author?.avatar?.url)
                    footer("ID: ${message.author?.id}")
                    timestamp = Instant.now()
                    color = Colors.ERROR.color
                }
            }
        }
    }

    private suspend fun cacheMessage(message: Message) {
        mutex.withLock {
            var size = cachedMessages.size
            if (size > 180) {
                val iterator = cachedMessages.iterator()
                while (size > 100 && iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                    size--
                }
            }
        }

        if (message.content.isBlank()) return
        cachedMessages[message.id] = message
    }
}
