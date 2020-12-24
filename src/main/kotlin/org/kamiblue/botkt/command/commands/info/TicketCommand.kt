package org.kamiblue.botkt.command.commands.info

import kotlinx.coroutines.delay
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.permission.overwrite.RolePermissionOverwrite
import net.ayataka.kordis.entity.server.permission.overwrite.UserPermissionOverwrite
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import org.kamiblue.botkt.*
import org.kamiblue.botkt.Permissions.hasPermission
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.manager.managers.ConfigManager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageUtils.error
import org.kamiblue.botkt.utils.MessageUtils.success
import org.kamiblue.botkt.utils.StringUtils.elseEmpty
import org.kamiblue.botkt.utils.prettyFormat
import org.kamiblue.event.listener.asyncListener
import java.io.File
import java.time.Instant

@Suppress("BlockingMethodInNonBlockingContext")
object TicketCommand : BotCommand(
    name = "ticket",
    description = "Manage tickets",
    category = Category.INFO
) {
    private val config = ConfigManager.readConfigSafe<TicketConfig>(ConfigType.TICKET, false)

    init {
        literal("close") {
            int("ticket number") { ticketNum ->
                executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Close a ticket") {
                    val ticket = message.server?.textChannels?.findByName("ticket-${ticketNum.value}") ?: run {
                        message.channel.error("Couldn't find a ticket named `ticket-${ticketNum.value}`!")
                        return@executeIfHas
                    }

                    ticket.delete()
                }
            }

            executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Close the current ticket") {
                if (message.serverChannel?.name?.startsWith("ticket-") != true) {
                    message.channel.error("The ${message.serverChannel?.mention} channel is not a ticket!")
                    return@executeIfHas
                }

                message.serverChannel?.delete()
            }
        }

        asyncListener<MessageReceiveEvent> { event ->
            val message = event.message
            val server = message.server ?: return@asyncListener
            val author = message.author ?: return@asyncListener
            val channel = message.serverChannel ?: return@asyncListener
            val ticketCategory = config?.ticketCategory?.let { server.channelCategories.find(it) } ?: return@asyncListener

            if (ticketCategory == channel.category) {
                if (channel.topic.isNullOrBlank()) {
                    Main.logger.error("Cannot log message from ticket ${channel.mention} / ${channel.name} due to missing channel topic")
                    return@asyncListener
                }

                val file = File("ticket_logs/" + channel.topic?.replace(" ", "_") + ".txt")
                val text = "[${message.timestamp.prettyFormat()}] [${author.mention}] " + message.content.elseEmpty("Message was empty!")

                if (!file.exists()) {
                    File("ticket_logs").mkdir()
                    file.createNewFile()
                    file.bufferedWriter().use { text }
                } else {
                    file.appendText(text + "\n")
                }
            }

            if (author.hasPermission(PermissionTypes.COUNCIL_MEMBER)) return@asyncListener

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
                    topic = "${author.id} ${Instant.now().prettyFormat()}"
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

                ticket.send(
                    "${author.mention} "
                        + config.ticketPingRole?.let { role ->
                        "<@&$role>"
                    }
                )

                ticket.send {
                    embed {
                        title = "Ticket Created!"
                        description = message.content
                        color = Colors.SUCCESS.color
                        footer("ID: ${author.id}", author.avatar.url)
                    }
                }

                val feedback = message.channel.success("${author.mention} Created ticket! Go to ${ticket.mention}!")
                delay(5000)
                feedback.delete()
                message.delete()
            }
        }
    }
}