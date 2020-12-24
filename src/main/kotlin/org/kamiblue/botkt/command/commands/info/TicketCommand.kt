package org.kamiblue.botkt.command.commands.info

import kotlinx.coroutines.delay
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
import org.kamiblue.botkt.*
import org.kamiblue.botkt.PermissionTypes.COUNCIL_MEMBER
import org.kamiblue.botkt.PermissionTypes.PURGE_PROTECTED
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.*
import org.kamiblue.botkt.utils.StringUtils.elseEmpty
import org.kamiblue.commons.extension.max
import org.kamiblue.event.listener.asyncListener
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.time.Instant

object TicketCommand : BotCommand(
    name = "ticket",
    description = "Manage tickets",
    category = Category.INFO
) {

    private val config get() = ConfigManager.readConfigSafe<TicketConfig>(ConfigType.TICKET, false)
    private val ticketFolder = File("ticket_logs")
    private val ticketFileRegex = "\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}_\\d{18}\\.".toRegex()

    private const val messageEmpty = "Message was empty!"

    init {
        literal("saveall") {
            executeIfHas(COUNCIL_MEMBER, "Saves the last 100 messages. Do not use on new tickets.") {
                val serverTextChannel = (channel as? ServerTextChannel)?: return@executeIfHas
                val msgs = channel.getMessages().reversed()
                val response = channel.normal("Saving `${msgs.size}` messages...")

                msgs.forEach {
                    delay(100) // my poor ssd
                    logTicket(
                        timeAndAuthor(it.author, it.timestamp) + it.content.elseEmpty(messageEmpty),
                        serverTextChannel,
                        it.author
                    )
                }

                response.edit {
                    description = "Saved `${msgs.size}` messages for ticket `${message.serverChannel?.topic}`!"
                    color = Colors.SUCCESS.color
                }
            }
        }

        literal("upload") {
            int("index") { indexArg ->
                executeIfHas(COUNCIL_MEMBER, "Upload a closed ticket file") {
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
                executeIfHas(PURGE_PROTECTED, "Delete a closed ticket entirely") {
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
                executeIfHas(COUNCIL_MEMBER, "View a closed ticket") {
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
            executeIfHas(COUNCIL_MEMBER, "List closed tickets") {
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
                executeIfHas(COUNCIL_MEMBER, "Close a ticket") {
                    val ticket = message.server?.textChannels?.findByName("ticket-${ticketNum.value}") ?: run {
                        channel.error("Couldn't find a ticket named `ticket-${ticketNum.value}`!")
                        return@executeIfHas
                    }

                    closeTicket(message, ticket)
                }
            }

            executeIfHas(COUNCIL_MEMBER, "Close the current ticket") {
                val channel = message.serverChannel

                if (channel?.name?.startsWith("ticket-") != true) {
                    channel?.error("The ${message.serverChannel?.mention} channel is not a ticket!")
                    return@executeIfHas
                }

                closeTicket(message, channel)
            }
        }

        asyncListener<MessageReceiveEvent> { event ->
            val server = event.server ?: return@asyncListener
            val channel = event.message.serverChannel ?: return@asyncListener
            val message = event.message
            val author = message.author ?: return@asyncListener
            val ticketCategory =
                config?.ticketCategory?.let { server.channelCategories.find(it) } ?: return@asyncListener

            if (ticketCategory == channel.category) {
                logTicket(
                    timeAndAuthor(author, message.timestamp) + message.content.elseEmpty(messageEmpty),
                    channel,
                    author
                )
            }

            if (!author.hasPermission(COUNCIL_MEMBER) && channel.id == config?.ticketCreateChannel) {
                if (author.bot) {
                    // We clean up our own messages later
                    if (author.id != Main.client.botUser.id) message.delete()
                    return@asyncListener
                }

                createTicket(server, channel, message, author, ticketCategory)
            }
        }
    }

    private suspend fun createTicket(
        server: Server,
        channel: ServerTextChannel,
        message: Message,
        author: User,
        ticketCategory: ChannelCategory
    ) {
        val tickets = server.textChannels.filter { it.category == ticketCategory }

        val ticket = server.createTextChannel {
            name = "ticket-${tickets.size}"
            topic = "${Instant.now().prettyFormat()} ${author.id}"
            category = ticketCategory
        }

        ticket.setPermissions(author)

        logTicket(
            "${timeAndAuthor(author, message.timestamp)}Created ticket: `${message.content}`".max(2048),
            ticket
        )

        ticket.send("${author.mention} <@&${config?.ticketPingRole}>")

        ticket.send {
            embed {
                title = "Ticket Created!"
                description = message.content
                color = Colors.SUCCESS.color
                footer("ID: ${author.id}", author.avatar.url)
            }
        }

        val feedback = channel.success("${author.mention} Created ticket! Go to ${ticket.mention}!")
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

    private suspend fun closeTicket(message: Message, channel: ServerTextChannel) {
        logTicket(
            "${timeAndAuthor(message.author, message.timestamp)}Closed ticket `${channel.topic}`",
            channel
        )
        channel.delete()
    }

    private fun logTicket(content: String, channel: ServerTextChannel, author: User? = null) {
        if (channel.topic.isNullOrBlank()) {
            Main.logger.error("Cannot log message from ticket ${channel.mention} / ${channel.name} due to missing channel topic")
            return
        }

        val ticketName = if (channel.id == config?.ticketCreateChannel) channel.name
        else channel.topic!!.replace(" ", "_").replace(":", ".")

        val file = File("${ticketFolder.name}/$ticketName.txt")

        if (author?.id == Main.client.botUser.id && content.endsWith(messageEmpty)) return

        if (!file.exists()) {
            ticketFolder.mkdir()
            file.createNewFile()
        }

        file.appendText(content + "\n")
    }

    private fun String.formatName(): String {
        if (length < 38) return this.removeSuffix(".txt")

        val id = substring(20, 38).toLongOrNull() ?: return this.removeSuffix(".txt")
        val time = substring(0, 19).replace("_", " ").replace(".", ":")

        return "$time <@!$id>"
    }

    private fun timeAndAuthor(author: User?, timestamp: Instant) = "[${timestamp.prettyFormat()}] [${author?.mention}] "

    private fun getTickets() = ticketFolder.listFiles(FileFilter { it.isFile && it.name.matches(ticketFileRegex) })!!.toList()

    private suspend fun MessageExecuteEvent.indexNotFound(index: Int) {
        channel.error("Ticket with index `$index` could not be found")
    }
}