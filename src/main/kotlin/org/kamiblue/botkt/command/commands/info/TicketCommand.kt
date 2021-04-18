package org.kamiblue.botkt.command.commands.info

import kotlinx.coroutines.*
import net.ayataka.kordis.entity.everyone
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.message.MessageBuilder
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.channel.category.ChannelCategory
import net.ayataka.kordis.entity.server.channel.text.ServerTextChannel
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.permission.overwrite.RolePermissionOverwrite
import net.ayataka.kordis.entity.server.permission.overwrite.UserPermissionOverwrite
import net.ayataka.kordis.entity.server.role.Role
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import net.ayataka.kordis.exception.NotFoundException
import org.kamiblue.botkt.*
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.command.options.HasPermission
import org.kamiblue.botkt.command.options.ServerOnly
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

// TODO: Move to plugin when plugins support custom config types
object TicketCommand : BotCommand(
    name = "ticket",
    description = "Manage tickets",
    category = Category.INFO
) {

    private val config get() = ConfigManager.readConfigSafe<TicketConfig>(ConfigType.TICKET, false)
    private val ticketFolder = File("ticket_logs")
    private val ticketFileRegex = "^\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}_\\d{18}\\.txt".toRegex()
    private val ticketIOScope = CoroutineScope(Dispatchers.IO + CoroutineName("Ticket IO"))
    private var cachedMessages = HashMap<ServerTextChannel, StringBuilder>()
    private var cachedOpenedTicketRole: Role? = null

    private const val messageEmpty = "Message was empty!"

    init {
        if (!ticketFolder.exists()) ticketFolder.mkdir()

        literal("saveall") {
            execute("Saves the last 100 messages. Do not use on new tickets.", HasPermission.get(PermissionTypes.COUNCIL_MEMBER)) {
                val channel = (channel as? ServerTextChannel) ?: return@execute
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
                                "Saved `${messages.size}` messages for ticket `${message.serverChannel?.topic?.topicTime()}`!"
                            color = Colors.SUCCESS.color
                        }
                    }
                }
            }
        }

        literal("upload") {
            int("index") { indexArg ->
                execute("Upload a closed ticket file", HasPermission.get(PermissionTypes.COUNCIL_MEMBER)) {
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
                execute("Delete a closed ticket entirely", HasPermission.get(PermissionTypes.PURGE_PROTECTED)) {
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
                execute("View a closed ticket", HasPermission.get(PermissionTypes.COUNCIL_MEMBER)) {
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

        literal("close") {
            execute("Close the current ticket", ServerOnly) {
                val channel = message.serverChannel
                val server = server!!

                if (channel?.name?.startsWith("ticket-") != true || channel.category?.id != config?.ticketCategory ?: 0) {
                    channel?.error("The ${message.serverChannel?.mention} channel is not a ticket!")
                    return@execute
                }

                val userID = channel.topic?.substring(20, 38)?.replace(notNumberRegex, "")?.toLongOrNull()

                closeTicket(channel, message)

                fixOpenedTicketRole(server)

                // Very scuffed
                cachedOpenedTicketRole?.let { role ->
                    userID?.let {
                        Main.client.getUser(it)?.toMember(server)?.removeRole(role)
                    }
                }
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

        BackgroundScope.launchLooping("Ticket saving", 1800000L) {
            saveAll()
            Main.logger.debug("Saved tickets")
        }

        listener<ShutdownEvent> {
            runBlocking {
                saveAll()
            }
        }
    }

    private fun getTickets() =
        ticketFolder.listFiles(FileFilter { it.isFile && it.name.matches(ticketFileRegex) })!!.toList().sortedBy { it.name }

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
        val config = config ?: return // lazy
        fixOpenedTicketRole(server)

        config.ticketTotalAmount = config.ticketTotalAmount?.let { it + 1 } ?: 1

        val ticketChannel = server.createTextChannel {
            name = "ticket-${config.ticketTotalAmount}"
            topic = "${Instant.now().prettyFormat()} ${author.id} ${message.content}".max(1024)
            category = ticketCategory
        }

        ticketChannel.setPermissions(author)

        logMessage(ticketChannel, message, "Created ticket: `${message.content}`".max(2048))

        ticketChannel.send("${author.mention} <@&${config.ticketPingRole}>")

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

        cachedOpenedTicketRole?.let { author.toMember(server)?.addRole(it) }
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
        logMessage(channel, message, "Closed ticket `${channel.topic?.topicTime()}`")
        cachedMessages.remove(channel)?.let {
            ticketIOScope.launch { saveChannel(channel, it, true) }
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
        ConfigManager.writeConfig(ConfigType.TICKET)

        val prev = cachedMessages
        cachedMessages = HashMap()

        prev.map { (channel, stringBuilder) ->
            ticketIOScope.async { saveChannel(channel, stringBuilder) }
        }.awaitAll()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun saveChannel(channel: ServerTextChannel, stringBuilder: StringBuilder, uploadFile: Boolean = false) {
        val ticketName = if (channel.id == config?.ticketCreateChannel) channel.name
        else channel.topic!!.topicTime().replace(" ", "_").replace(":", ".")

        if (!ticketFolder.exists()) ticketFolder.mkdir()

        val file = File("${ticketFolder.name}/$ticketName.txt")

        file.run {
            if (!exists()) createNewFile()
            appendText(stringBuilder.toString())
        }

        if (uploadFile) {
            val user = channel.topic?.substring(20, 38)?.replace(notNumberRegex, "")?.toLongOrNull()?.let {
                Main.client.getUser(it)
            }

            val users = arrayListOf<Long>()
            file.readLines().forEach {
                try {
                    val ticketUser = it.substring(25, 43).toLongOrNull() ?: return@forEach
                    if (ticketUser != Main.client.botUser.id && !users.contains(ticketUser)) {
                        users.add(ticketUser)
                    }
                } catch (e: StringIndexOutOfBoundsException) {
                    Main.logger.debug(e)
                }
            }

            config?.ticketUploadChannel?.let { uploadChannelConfig ->
                val uploadChannel = channel.server.textChannels.find(uploadChannelConfig) ?: run {
                    Main.logger.warn("Invalid ticketUploadChannel set")
                    return
                }

                val embed = MessageBuilder().apply {
                    embed {
                        field("Ticket Name", "`${channel.name}`")
                        channel.topic?.substring(39)?.let { field("Topic", "`$it`") }
                        field("Author", user?.mention.toString())
                        channel.topic?.substring(0, 19)?.let { field("Opened Date", it) }
                        field("Participants", users.joinToString("\n") { "<@!$it>" })
                        footer("ID: ${user?.id}", user?.avatar?.url)
                        color = Colors.PRIMARY.color
                    }
                }.build()

                uploadChannel.upload(file, embed = embed)
                user?.directMessagesSafe()?.upload(file, embed = embed)
            } ?: run {
                Main.logger.warn("Couldn't upload ticket file because ticketUploadChannel is not set")
            }
        }
    }

    private fun fixOpenedTicketRole(server: Server) {
        if (cachedOpenedTicketRole != null) return
        val id = config?.ticketOpenedRole ?: return

        server.roles.find { role -> role.id == id }?.let {
            cachedOpenedTicketRole = it
        }
    }

    private fun String.topicTime() = this.max(38)
}
