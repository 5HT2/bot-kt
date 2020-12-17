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
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.success
import org.kamiblue.botkt.utils.SnowflakeHelper.prettyFormat
import org.kamiblue.event.listener.asyncListener
import java.time.Instant

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
                        message.error("Couldn't find a ticket named `ticket-${ticketNum.value}`!")
                        return@executeIfHas
                    }

                    message.delete() // audit log who ran the command
                    ticket.delete()
                }
            }

            executeIfHas(PermissionTypes.COUNCIL_MEMBER, "Close the current ticket") {
                if (message.serverChannel?.name?.startsWith("ticket-") != true) {
                    message.error("The ${message.serverChannel?.mention} channel is not a ticket!")
                    return@executeIfHas
                }

                message.delete() // audit log who ran the command
                message.serverChannel?.delete()
            }
        }

        asyncListener<MessageReceiveEvent> { event ->
            val server = event.message.server
            val author = event.message.author

            if (author.hasPermission(PermissionTypes.COUNCIL_MEMBER)) return@asyncListener

            config?.ticketCreateChannel?.let {
                if (event.message.channel.id != it) return@asyncListener

                if (author?.bot == true && author.id != Main.client.botUser.id) {
                    event.message.delete()
                    return@asyncListener
                }

                val everyone = event.message.server?.id?.let { serverId -> event.message.server?.roles?.find(serverId) }
                    ?: return@asyncListener

                config.ticketCategory?.let { configTicketCategory ->
                    val ticketCategory = server?.channelCategories?.find(configTicketCategory) ?: return@asyncListener
                    val tickets = server.textChannels.filter { channels -> channels.category == ticketCategory }

                    val ticket = server.createTextChannel {
                        name = "ticket-${tickets.size}"
                        topic = "${author?.id} ${Instant.now().prettyFormat()}"
                        category = ticketCategory
                    }

                    ticket.edit {
                        userPermissionOverwrites.add(UserPermissionOverwrite(author!!, PermissionSet(117760)))
                        rolePermissionOverwrites.add(
                            RolePermissionOverwrite(
                                everyone,
                                PermissionSet(0),
                                PermissionSet(3072)
                            )
                        )
                    }

                    ticket.send(
                        "${author?.mention} "
                            + config.ticketPingRole?.let { role ->
                            "<@&$role>"
                        }
                    )

                    ticket.send {
                        embed {
                            title = "Ticket Created!"
                            description = event.message.content
                            color = Colors.SUCCESS.color
                            footer("ID: ${author?.id}", author?.avatar?.url)
                        }
                    }

                    val feedback = event.message.success("${author?.mention} Created ticket! Go to ${ticket.mention}!")
                    delay(5000)
                    feedback.delete()
                    event.message.delete()
                }
            }
        }
    }
}