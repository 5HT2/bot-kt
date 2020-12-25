package org.kamiblue.botkt.command.commands.info

import kotlinx.coroutines.*
import net.ayataka.kordis.entity.everyone
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.channel.category.ChannelCategory
import net.ayataka.kordis.entity.server.channel.text.ServerTextChannel
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.permission.overwrite.RolePermissionOverwrite
import net.ayataka.kordis.entity.server.permission.overwrite.UserPermissionOverwrite
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import net.ayataka.kordis.exception.NotFoundException
import org.kamiblue.botkt.*
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.event.events.ShutdownEvent
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.*
import org.kamiblue.botkt.utils.StringUtils.elseEmpty
import org.kamiblue.commons.extension.max
import org.kamiblue.event.listener.asyncListener
import org.kamiblue.event.listener.listener
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap

object TicketCommand : BotCommand(
    name = "ticket",
    description = "Manage tickets",
    category = Category.INFO
) {

    private val config get() = ConfigManager.readConfigSafe<TicketConfig>(ConfigType.TICKET, false)
    private val ticketFolder = File("ticket_logs")
    private val ticketFileRegex = "\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}_\\d{18}\\.".toRegex()
    private val ticketIOScope = CoroutineScope(Dispatchers.IO + CoroutineName("Ticket IO"))
    private var cachedMessages = HashMap<ServerTextChannel, StringBuilder>()

    private const val messageEmpty = "Message was empty!"

    init {
        if (!ticketFolder.exists()) ticketFolder.mkdir()

        literal("saveall") {
            executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Saves the last 100 messages. Do not use on new tickets.") {
                val channel = (channel as? ServerTextChannel) ?: return@executeIfHas
                val messages = this.channel.getMessages().reversed()
                val response = this.channel.normal("Saving `${messages.size}` messages...")

                messages.forEach {
                    logMessage(channel, it, it.content.elseEmpty(messageEmpty))
                }

                cachedMessages.remove(channel)?.let {
                    ticketIOScope.launch {
                        saveChannel(channel, it)
                        response.edit {
                            description =
                                "Saved `${messages.size}` messages for ticket `${message.serverChannel?.topic}`!"
                            color = Colors.SUCCESS.color
                        }
                    }
                }
            }
        }

        literal("upload") {
            int("index") { indexArg ->
                executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Upload a closed ticket file") {
                    val index = indexArg.value
                    try {
                        channel.upload(getTickets()[index])
                    } catch (e: IndexOutOfBoundsException) {
                        indexNotFound(index)
                    } catch (e: FileNotFoundException) {
                        indexNotFound(index)
                    }
                }
            }
        }

        literal("delete") {
            int("index") { indexArg ->
                executeIfHas(PermissionTypes.PURGE_PROTECTED, "Delete a closed ticket entirely") {
                    try {
                        getTickets().getOrNull(indexArg.value)?.delete()
                        channel.success("Deleted ticket with index `${indexArg.value}`")
                    } catch (e: FileNotFoundException) {
                        indexNotFound(indexArg.value)
                    }
                }
            }
        }

        literal("view") {
            int("index") { indexArg ->
                executeIfHas(PermissionTypes.COUNCIL_MEMBER, "View a closed ticket") {
                    val index = indexArg.value
                    try {
                        channel.send {
                            embed {
                                joinToFields(getTickets()[index].readLines(), "\n")
                                color = Colors.PRIMARY.color
                            }
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        indexNotFound(index)
                    } catch (e: FileNotFoundException) {
                        indexNotFound(index)
                    }
                }
            }
        }

        literal("list") {
            executeIfHas(PermissionTypes.COUNCIL_MEMBER, "List closed tickets") {
                channel.send {
                    embed {
                        joinToFields(getTickets().withIndex(), "\n") {
                            "`${it.index}`: ${it.value.name.formatName()}\n"
                        }
                        color = Colors.PRIMARY.color
                    }
                }
            }
        }

        literal("close") {
            int("ticket number") { ticketNum ->
                executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Close a ticket") {
                    val ticketChannel = message.server?.textChannels?.findByName("ticket-${ticketNum.value}") ?: run {
                        channel.error("Couldn't find a ticket named `ticket-${ticketNum.value}`!")
                        return@executeIfHas
                    }

                    closeTicket(ticketChannel, message)
                }
            }

            executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Close the current ticket") {
                val channel = message.serverChannel

                if (channel?.name?.startsWith("ticket-") != true) {
                    channel?.error("The ${message.serverChannel?.mention} channel is not a ticket!")
                    return@executeIfHas
                }

                closeTicket(channel, message)
            }
        }

        asyncListener<MessageReceiveEvent> { event ->
            val server = event.server ?: return@asyncListener
            val channel = event.message.serverChannel ?: return@asyncListener
            val message = event.message
            val author = message.author ?: return@asyncListener
            val category = channel.category ?: return@asyncListener

            if (category.id != config?.ticketCategory) return@asyncListener
            if (message.content.isBlank() && author == Main.client.botUser) return@asyncListener

            logMessage(channel, message, message.content.elseEmpty(messageEmpty))

            if (!author.hasPermission(PermissionTypes.COUNCIL_MEMBER) && channel.id == config?.ticketCreateChannel) {
                if (author.bot) {
                    if (author.id != Main.client.botUser.id) {
                        message.delete() // remove other bot messages
                    } else {
                        delay(10000)
                        try {
                            message.delete()
                        } catch (ignored: NotFoundException) {
                            // clean up any error or accidental messages that we sent
                        }
                    }
                    return@asyncListener
                }

                createTicket(server, channel, message, author, category)
            }
        }

        BackgroundScope.add(1800000L) {
            saveAll()
        }

        listener<ShutdownEvent> {
            runBlocking {
                saveAll()
            }
        }
    }

    private fun String.formatName(): String {
        if (length < 38) return this.removeSuffix(".txt")

        val id = substring(20, 38).toLongOrNull() ?: return this.removeSuffix(".txt")
        val time = substring(0, 19).replace("_", " ").replace(".", ":")

        return "$time <@!$id>"
    }

    private fun getTickets() =
        ticketFolder.listFiles(FileFilter { it.isFile && it.name.matches(ticketFileRegex) })!!.toList()

    private suspend fun MessageExecuteEvent.indexNotFound(index: Int) {
        channel.error("Ticket with index `$index` could not be found")
    }

    private suspend fun createTicket(
        server: Server,
        channel: ServerTextChannel,
        message: Message,
        author: User,
        ticketCategory: ChannelCategory
    ) {
        val tickets = server.textChannels.filter { it.category == ticketCategory }

        val ticketChannel = server.createTextChannel {
            name = "ticket-${tickets.size}"
            topic = "${Instant.now().prettyFormat()} ${author.id}"
            category = ticketCategory
        }

        ticketChannel.setPermissions(author)

        logMessage(ticketChannel, message, "Created ticket: `${message.content}`".max(2048))

        ticketChannel.send("${author.mention} <@&${config?.ticketPingRole}>")

        ticketChannel.send {
            embed {
                title = "Ticket Created!"
                description = message.content
                color = Colors.SUCCESS.color
                footer("ID: ${author.id}", author.avatar.url)
            }
        }

        val feedback = channel.success("${author.mention} Created ticket! Go to ${ticketChannel.mention}!")
        delay(5000)
        feedback.delete()
        message.delete()
    }

    private suspend fun ServerTextChannel.setPermissions(author: User) {
        edit {
            userPermissionOverwrites.add(UserPermissionOverwrite(author, PermissionSet(117760)))
            rolePermissionOverwrites.add(
                RolePermissionOverwrite(
                    server.roles.everyone,
                    PermissionSet(0),
                    PermissionSet(3072)
                )
            )
        }
    }

    private suspend fun closeTicket(channel: ServerTextChannel, message: Message) {
        logMessage(channel, message, "Closed ticket `${channel.topic}`")
        cachedMessages.remove(channel)?.let {
            ticketIOScope.launch { saveChannel(channel, it) }
        }
        channel.delete()
    }

    private fun logMessage(channel: ServerTextChannel, message: Message, content: String) {
        if (channel.topic.isNullOrBlank()) {
            Main.logger.warn("Cannot log message from ticket ${channel.mention} / ${channel.name} due to missing channel topic")
        } else {
            cachedMessages.getOrPut(channel, ::StringBuilder).apply {
                append(formatMessage(message))
                appendLine(content)
            }
        }
    }

    private fun formatMessage(message: Message) = "[${message.timestamp.prettyFormat()}] [${message.author?.mention}] "

    private suspend fun saveAll() {
        val prev = cachedMessages
        cachedMessages = HashMap()

        prev.map { (channel, stringBuilder) ->
            ticketIOScope.async { saveChannel(channel, stringBuilder) }
        }.awaitAll()
    }

    private fun saveChannel(channel: ServerTextChannel, stringBuilder: java.lang.StringBuilder) {
        val ticketName = if (channel.id == config?.ticketCreateChannel) channel.name
        else channel.topic!!.replace(" ", "_").replace(":", ".")

        File("${ticketFolder.name}/$ticketName.txt").run {
            if (!exists()) {
                mkdir()
                createNewFile()
            }
            appendText(stringBuilder.toString())
        }
    }

}