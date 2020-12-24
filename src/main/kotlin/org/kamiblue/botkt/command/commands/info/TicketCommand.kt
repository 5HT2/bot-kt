package org.kamiblue.botkt.command.commands.info

import kotlinx.coroutines.delay
import net.ayataka.kordis.entity.message.Message
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
import org.kamiblue.botkt.helpers.ShellHelper.systemBash
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageUtils.error
import org.kamiblue.botkt.utils.MessageUtils.normal
import org.kamiblue.botkt.utils.MessageUtils.success
import org.kamiblue.botkt.utils.StringUtils.elseEmpty
import org.kamiblue.botkt.utils.prettyFormat
import org.kamiblue.commons.extension.max
import org.kamiblue.event.listener.asyncListener
import java.io.File
import java.io.FileNotFoundException
import java.time.Instant

@Suppress("BlockingMethodInNonBlockingContext")
object TicketCommand : BotCommand(
    name = "ticket",
    description = "Manage tickets",
    category = Category.INFO
) {
    private val config = ConfigManager.readConfigSafe<TicketConfig>(ConfigType.TICKET, false)
    private val fileExtension = Regex(".txt$")
    private val ticketFolder = File("ticket_logs")
    private const val messageEmpty = "Message was empty!"

    init {
        literal("saveall") {
            executeIfHas(COUNCIL_MEMBER, "Saves the last 100 messages. Do not use on new tickets.") {
                val msgs = channel.getMessages().reversed()
                val response = channel.normal("Saving `${msgs.size}` messages...")

                msgs.forEach {
                    delay(100) // my poor ssd
                    logTicket(
                        timeAndAuthor(it.author, it.timestamp) + it.content.elseEmpty(messageEmpty),
                        it.serverChannel,
                        it.author
                    )
                }

                response.edit {
                    description = "Saved `${msgs.size}` messages for ticket `${message.serverChannel?.topic}`!"
                }
            }
        }

        literal("upload") {
            int("index") { indexArg ->
                executeIfHas(COUNCIL_MEMBER, "Upload a closed ticket file") {
                    try {
                        getTickets().getOrNull(indexArg.value)?.let { // TODO: switch to proper file uploading when switching to JDA
                            config?.ticketUploadChannel?.let { webhook ->
                                "curl -F content=@\"${it.path}\" \"$webhook\"".systemBash()
                                channel.success("Uploaded ticket with index `${indexArg.value}`")
                            } ?: run {
                                channel.error("`ticketUploadChannel` is not set")
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        indexNotFound(indexArg.value)
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
                    try {
                        getTickets().getOrNull(indexArg.value)?.let {
                            val formatted = it.readLines().joinToString("\n").chunked(1024)

                            channel.send {
                                embed {
                                    formatted.withIndex().forEach { contents ->
                                        field("${contents.index + 1} / ${formatted.size}", contents.value.elseEmpty("Empty"))
                                    }
                                    color = Colors.PRIMARY.color
                                }
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        indexNotFound(indexArg.value)
                    }
                }
            }
        }

        literal("list") {
            executeIfHas(COUNCIL_MEMBER, "List closed tickets") {
                val tickets = getTickets().withIndex()
                val ticketNames = tickets.joinToString("\n") {
                    if (it.value.name == ticketFolder.name) "" else "`${it.index}`: " + it.value.name.formatName()
                }.chunked(1024)

                channel.send {
                    embed {
                        ticketNames.withIndex().forEach { contents ->
                            field("${contents.index + 1} / ${ticketNames.size}", contents.value.elseEmpty("Empty"))
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
                    channel.error("The ${message.serverChannel?.mention} channel is not a ticket!")
                    return@executeIfHas
                }

                closeTicket(message, channel)
            }
        }

        asyncListener<MessageReceiveEvent> { event ->
            val message = event.message
            val server = message.server ?: return@asyncListener
            val author = message.author ?: return@asyncListener
            val channel = message.serverChannel ?: return@asyncListener
            val ticketCategory = config?.ticketCategory?.let { server.channelCategories.find(it) } ?: return@asyncListener

            if (ticketCategory == channel.category) {
                logTicket(
                    timeAndAuthor(author, message.timestamp) + message.content.elseEmpty(messageEmpty),
                    channel,
                    author
                )
            }

            if (author.hasPermission(COUNCIL_MEMBER)) return@asyncListener

            config.ticketCreateChannel?.let {
                if (message.channel.id != it) return@asyncListener

                if (author.bot) {
                    if (author.id != Main.client.botUser.id) {
                        message.delete() // we clean up our own messages later
                    }
                    return@asyncListener
                }

                val everyone = message.server?.id?.let { serverId -> message.server?.roles?.find(serverId) }
                    ?: return@asyncListener

                val tickets = server.textChannels.filter { channels -> channels.category == ticketCategory }

                val ticket = server.createTextChannel {
                    name = "ticket-${tickets.size}"
                    topic = "${Instant.now().prettyFormat()} ${author.id}"
                    category = ticketCategory
                }

                ticket.edit {
                    userPermissionOverwrites.add(UserPermissionOverwrite(author, PermissionSet(117760)))
                    rolePermissionOverwrites.add(
                        RolePermissionOverwrite(
                            everyone,
                            PermissionSet(0),
                            PermissionSet(3072)
                        )
                    )
                }

                logTicket("${timeAndAuthor(author, message.timestamp)}Created ticket: `${message.content}`".max(2048), ticket)

                ticket.send(
                    "${author.mention} "
                        + "<@&${config.ticketPingRole}>"
                )

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
        }
    }

    private suspend fun closeTicket(message: Message, channel: ServerTextChannel) {
        logTicket("${timeAndAuthor(message.author, message.timestamp)}Closed ticket `${channel.topic}`", message.serverChannel)
        channel.delete()
    }

    private fun logTicket(content: String, channel: ServerTextChannel?, author: User? = null) {
        if (channel?.topic.isNullOrBlank()) {
            Main.logger.error("Cannot log message from ticket ${channel?.mention} / ${channel?.name} due to missing channel topic")
            return
        }

        channel ?: return

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
        if (length < 38) return this.replace(fileExtension, "")

        val id = substring(20, 38).toLongOrNull() ?: return this.replace(fileExtension, "")
        val time = substring(0, 19).replace("_", " ").replace(".", ":")

        return "$time <@!$id>"
    }

    private fun timeAndAuthor(author: User?, timestamp: Instant) = "[${timestamp.prettyFormat()}] [${author?.mention}] "

    private fun getTickets() = ticketFolder.walkTopDown().sortedBy { it.name }.toList()

    private suspend fun MessageExecuteEvent.indexNotFound(index: Int) {
        channel.error("Ticket with index `$index` could not be found")
    }
}