package org.kamiblue.botkt.command.commands.moderation

import net.ayataka.kordis.entity.edit
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.channel.ServerChannel
import net.ayataka.kordis.entity.server.channel.text.ServerTextChannel
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.permission.overwrite.RolePermissionOverwrite
import org.kamiblue.botkt.*
import org.kamiblue.botkt.PermissionTypes.*
import org.kamiblue.botkt.command.*
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.MessageUtils.error
import org.kamiblue.botkt.utils.MessageUtils.normal
import org.kamiblue.botkt.utils.MessageUtils.success
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.botkt.utils.pretty
import kotlin.collections.set

object ChannelCommand : BotCommand(
    name = "channel",
    alias = arrayOf("ch"),
    category = Category.MODERATION,
    description = "Modify, copy, save, archive and slow channels"
) {
    private val permissions = HashMap<String, Collection<RolePermissionOverwrite>>()
    private val rolePermHistory = HashMap<ServerChannel, ArrayDeque<List<RolePermissionOverwrite>>>()

    private var previousChange: Triple<Pair<ChangeType, String>, ServerChannel, Collection<RolePermissionOverwrite>>? = null

    init {
        literal("save") {
            executeIfHas(MANAGE_CHANNELS, "Save a channel's permissions") {
                val serverChannel = message.serverChannel(message) ?: return@executeIfHas
                val name = serverChannel.name

                save(name, serverChannel, message)
            }

            string("name") { name ->
                executeIfHas(MANAGE_CHANNELS, "Save a channel's permissions") {
                    val serverChannel = message.serverChannel(message) ?: return@executeIfHas

                    save(name.value, serverChannel, message)
                }
            }
        }

        literal("print") {
            executeIfHas(MANAGE_CHANNELS, "Print the current channels permissions") {
                val c = message.serverChannel(message) ?: return@executeIfHas
                val name = c.name

                print(name, message)
            }

            string("name") { name ->
                executeIfHas(MANAGE_CHANNELS, "Print a channels permissions") {
                    print(name.value, message)
                }
            }
        }

        literal("load") {
            string("name") { name ->
                executeIfHas(MANAGE_CHANNELS, "Load a channels permissions from saved") {
                    load(name.value, message)
                }
            }
        }

        @Suppress("UNREACHABLE_CODE") // TODO: Doesn't work
        literal("undo") {
            executeIfHas(MANAGE_CHANNELS, "Undo the last change to channels") {
                channel.error("Undo isn't fully supported yet!")
                return@executeIfHas

                undo(message)
            }
        }

        literal("sync") {
            literal("category") {
                executeIfHas(MANAGE_CHANNELS, "Sync category permissions to channel permissions") {
                    val c = message.serverChannel(message) ?: return@executeIfHas

                    sync(true, message, c)
                }
            }

            executeIfHas(MANAGE_CHANNELS, "Sync channel permissions to category permissions") {
                val c = message.serverChannel(message) ?: return@executeIfHas

                sync(false, message, c)
            }
        }

        literal("slow") {
            executeIfHas(COUNCIL_MEMBER, "Remove slowmode for the current channel") {
                message.serverChannel?.let {
                    it.edit {
                        rateLimitPerUser = 0
                    }

                    channel.success("Removed slowmode")
                } ?: run {
                    channel.error("Server channel is null, are you running this from a DM?")
                }
            }

            int("wait") { waitArg ->
                executeIfHas(COUNCIL_MEMBER, "Set slowmode for the current channel") {
                    val wait = waitArg.value

                    message.serverChannel?.let {
                        it.edit {
                            rateLimitPerUser = wait
                        }

                        channel.success(if (wait != 0) "Set slowmode to ${wait}s" else "Removed slowmode")
                    } ?: run {
                        channel.error("Server channel is null, are you running this from a DM?")
                    }
                }

            }
        }

        literal("archive") {
            executeIfHas(ARCHIVE_CHANNEL, "Archive the current channel") {
                val c = message.serverChannel(message) ?: return@executeIfHas
                val s = server ?: run { channel.error("Server is null, are you running this from a DM?"); return@executeIfHas }
                val everyone = s.roles.find(s.id)!! // this cannot be null, as it's the @everyone role and we already checked server null
                val oldName = c.name

                val archivedChannels = s.channels.filter { n -> n.name.contains(Regex("archived-[0-9]+")) }
                val totalArchived = archivedChannels.size

                c.edit {
                    userPermissionOverwrites.clear()
                    rolePermissionOverwrites.clear()
                    rolePermissionOverwrites.add(RolePermissionOverwrite(everyone, PermissionSet(0), PermissionSet(1024))) // disallow read for everyone
                    name = "archived-$totalArchived"
                }

                channel.success("Changed name from `$oldName` to `archived-$totalArchived`")

            }
        }

        literal("lock") {
            literal("category") {
                executeIfHas(COUNCIL_MEMBER, "Lock all channels in the category") {
                    lockOrUnlock(category = true, lock = true, message, server)
                }
            }

            executeIfHas(COUNCIL_MEMBER, "Lock the current channel") {
                lockOrUnlock(category = false, lock = true, message, server)
            }
        }

        literal("unlock") {
            literal("category") {
                executeIfHas(COUNCIL_MEMBER, "Unlock all the channels in the category") {
                    lockOrUnlock(category = true, lock = false, message, server)
                }
            }

            executeIfHas(COUNCIL_MEMBER, "Unlock the current channel") {
                lockOrUnlock(category = false, lock = false, message, server)
            }
        }
    }

    private suspend fun save(saveName: String, serverChannel: ServerChannel, message: Message) {
        val selectedConfig = ArrayList(serverChannel.rolePermissionOverwrites)

        previousChange = Triple(Pair(ChangeType.SAVE, saveName), serverChannel, serverChannel.rolePermissionOverwrites)
        // make sure to run this AFTER saving previous state
        permissions[saveName] = selectedConfig

        channel.success("Saved current channel permissions, use `$name print $saveName` to print permissions!")
    }

    private suspend fun print(name: String, message: Message) {
        val selectedChannel = permissions[name] ?: run {
            channel.error("Couldn't find `$name` in saved channel presets!")
            return
        }

        val string = selectedChannel.joinToString(separator = "\n") {
            "${it.role.mention}\n" +
                "Allow: ${it.allow.pretty()}\n" +
                "Deny: ${it.deny.pretty()}\n"
        }

        permissions[name] = selectedChannel

        if (string.isBlank()) {
            channel.error("No saved permissions for `$name`!")
        } else channel.normal(string)
    }

    private suspend fun load(name: String, message: Message) {
        val selectedChannel = permissions[name] ?: run {
            channel.error("Couldn't find `$name` in saved channel presets!")
            return
        }

        val serverChannel = message.serverChannel(message) ?: return
        previousChange = Triple(Pair(ChangeType.LOAD, name), serverChannel, serverChannel.rolePermissionOverwrites)

        serverChannel.setPermissions(selectedChannel)

        channel.success("Loaded channel permissions from `$name`!")
    }

    private suspend fun undo(message: Message) {
        val m = channel.normal("Attempting to undo last change...")

        previousChange?.let {
            m.edit {
                description = "Attempting to undo last change...\nFound: ${it.second.name.toHumanReadable()}"
                color = Colors.PRIMARY.color
            }

            when (it.first.first) {
                ChangeType.SAVE -> {
                    permissions[it.first.second] = it.third

                    m.edit {
                        description = "Attempting to undo last change...\n" +
                            "Found: ${it.second.name.toHumanReadable()}\n\n" +
                            "Unsaved, set ${it.first.second} to original permissions"
                        color = Colors.SUCCESS.color
                    }

                    previousChange = null
                }

                ChangeType.LOAD -> {
                    it.second.setPermissions(it.third)

                    m.edit {
                        description = "Attempting to undo last change...\n" +
                            "Found: ${it.second.name.toHumanReadable()}\n\n" +
                            "Unloaded, set ${it.first.second} to original permissions"
                        color = Colors.SUCCESS.color
                    }

                    previousChange = null
                }
            }
        } ?: run {
            m.edit {
                description = "Attempting to undo last change...\nCouldn't find any recent changes"
            }
        }
    }

    private suspend fun sync(reverse: Boolean, message: Message, serverChannel: ServerChannel) {
        val category = message.serverChannel?.category
        val perms = category?.rolePermissionOverwrites ?: run {
            channel.error("Channel category is null! Are you running this from a DM?")
            return
        }

        if (reverse) {
            category.setPermissions(perms)
            channel.success("Synchronized category permissions to the `${serverChannel.name.toHumanReadable()}` channel!")
        } else {
            serverChannel.setPermissions(perms)
            channel.success("Synchronized channel permissions to the `${category.name.toHumanReadable()}` category!")
        }
    }

    private suspend fun lockOrUnlock(category: Boolean, lock: Boolean, message: Message, server: Server?) {
        if (server == null) {
            channel.error("Server is null, are you running this from a DM?")
            return
        }

        val everyone = server.roles.find(server.id)!! // this cannot be null, as it's the @everyone role and we already checked server null

        val channel = (if (category) message.serverChannel?.category else message.serverChannel)
            ?: run { channel.error("${if (category) "Category" else "Server channel"} was null, was you running this from a DM?"); return }

        val perm = RolePermissionOverwrite(everyone, PermissionSet(0), PermissionSet(2048))

        if (lock) {
            rolePermHistory.getOrPut(channel, ::ArrayDeque).apply {
                add(channel.rolePermissionOverwrites.toList())
                while (size > 5) this.removeFirst()
            }

            channel.edit {
                rolePermissionOverwrites.add(perm)
            }
            channel.success("Locked ${if (category) "category" else "channel"}!")
        } else {
            channel.tryGetPrevPerm()?.let {
                channel.setPermissions(it)
            } ?: channel.edit {
                rolePermissionOverwrites.remove(perm)
            }
            channel.success("Unlocked ${if (category) "category" else "channel"}!")
        }

    }

    private fun ServerChannel.tryGetPrevPerm(): Collection<RolePermissionOverwrite>? {
        return rolePermHistory[this]?.lastOrNull()
            ?: permissions[this.name]
            ?: if (this is ServerTextChannel) permissions[this.category?.name]
            else null
    }

    private suspend fun ServerChannel.setPermissions(permissions: Collection<RolePermissionOverwrite>) {
        this.edit {
            this.rolePermissionOverwrites.clear()
            this.rolePermissionOverwrites.addAll(permissions)
        }
    }

    private suspend fun Message.serverChannel(message: Message): ServerChannel? {
        val sc = this.server?.channels?.find(this.channel.id)

        if (sc == null) {
            channel.error("Channel is null! Are you running this from a DM?")
        }

        return sc
    }

    private enum class ChangeType {
        SAVE, LOAD
    }
}
