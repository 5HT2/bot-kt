package org.kamiblue.botkt.commands

import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import net.ayataka.kordis.entity.edit
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.channel.ServerChannel
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.permission.overwrite.RolePermissionOverwrite
import org.kamiblue.botkt.*
import org.kamiblue.botkt.PermissionTypes.*
import org.kamiblue.botkt.utils.MessageSendUtils.error
import org.kamiblue.botkt.utils.MessageSendUtils.normal
import org.kamiblue.botkt.utils.MessageSendUtils.success
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.botkt.utils.pretty
import kotlin.collections.set

object ChannelCommand : Command("channel") {
    init {
        literal("save") {
            doesLaterIfHas(MANAGE_CHANNELS) {
                val serverChannel = message.serverChannel(message) ?: run { return@doesLaterIfHas }
                val name = serverChannel.name

                save(name, serverChannel, message)
            }

            string("name") {
                doesLaterIfHas(MANAGE_CHANNELS) { context ->
                    val serverChannel = message.serverChannel(message) ?: run { return@doesLaterIfHas }
                    val name: String = context arg "name"

                    save(name, serverChannel, message)
                }
            }
        }

        literal("print") {
            doesLaterIfHas(MANAGE_CHANNELS) {
                val c = message.serverChannel(message) ?: run { return@doesLaterIfHas }
                val name = c.name

                print(name, message)
            }

            string("name") {
                doesLaterIfHas(MANAGE_CHANNELS) { context ->
                    val name: String = context arg "name"

                    print(name, message)
                }
            }
        }

        literal("load") {
            string("name") {
                doesLaterIfHas(MANAGE_CHANNELS) { context ->
                    val name: String = context arg "name"

                    load(name, message)
                }
            }
        }

        @Suppress("UNREACHABLE_CODE") // TODO: Doesn't work
        literal("undo") {
            doesLaterIfHas(MANAGE_CHANNELS) {
                message.error("Undo isn't fully supported yet!")
                return@doesLaterIfHas

                undo(message)
            }
        }

        literal("sync") {
            literal("category") {
                doesLaterIfHas(MANAGE_CHANNELS) {
                    val c = message.serverChannel(message) ?: run { return@doesLaterIfHas }

                    sync(true, message, c)
                }
            }

            doesLaterIfHas(MANAGE_CHANNELS) {
                val c = message.serverChannel(message) ?: run { return@doesLaterIfHas }

                sync(false, message, c)
            }
        }

        literal("slow") {
            doesLaterIfHas(COUNCIL_MEMBER) {
                message.serverChannel?.let {
                    it.edit {
                        rateLimitPerUser = 0
                    }

                    message.success("Removed slowmode")
                } ?: run {
                    message.error("Server channel is null, are you running this from a DM?")
                }
            }

            integer("wait") {
                doesLaterIfHas(COUNCIL_MEMBER) { context ->
                    val wait: Int = context arg "wait"

                    message.serverChannel?.let {
                        it.edit {
                            rateLimitPerUser = wait
                        }

                        message.success(if (wait != 0) "Set slowmode to ${wait}s" else "Removed slowmode")
                    } ?: run {
                        message.error("Server channel is null, are you running this from a DM?")
                    }
                }

            }
        }

        literal("archive") {
            doesLaterIfHas(ARCHIVE_CHANNEL) {
                val c = message.serverChannel(message) ?: run { return@doesLaterIfHas }
                val s = server ?: run { message.error("Server is null, are you running this from a DM?"); return@doesLaterIfHas }
                val everyone = s.roles.find(s.id)!! // this cannot be null, as it's the @everyone role and we already checked server null
                val oldName = c.name

                val archivedChannels = s.channels.filter { n -> n.name.contains(Regex("archived-[0-9]+")) }
                val totalArchived = archivedChannels.size

                c.edit {
                    userPermissionOverwrites.removeAll(userPermissionOverwrites)
                    rolePermissionOverwrites.removeAll(rolePermissionOverwrites)
                    rolePermissionOverwrites.add(RolePermissionOverwrite(everyone, PermissionSet(0), PermissionSet(1024))) // disallow read for everyone
                    name = "archived-$totalArchived"
                }

                message.success("Changed name from `$oldName` to `archived-$totalArchived`")

            }
        }

        literal("lock") {
            literal("category") {
                doesLaterIfHas(COUNCIL_MEMBER) {
                    lockOrUnlock(category = true, lock = true, message = message, server = server)
                }
            }

            doesLaterIfHas(COUNCIL_MEMBER) {
                lockOrUnlock(category = false, lock = true, message = message, server = server)
            }
        }

        literal("unlock") {
            literal("category") {
                doesLaterIfHas(COUNCIL_MEMBER) {
                    lockOrUnlock(category = true, lock = false, message = message, server = server)
                }
            }

            doesLaterIfHas(COUNCIL_MEMBER) {
                lockOrUnlock(category = false, lock = false, message = message, server = server)
            }
        }
    }

    private suspend fun save(name: String, serverChannel: ServerChannel, message: Message) {
        val selectedConfig = HashSet<RolePermissionOverwrite>()

        serverChannel.rolePermissionOverwrites.forEach {
            selectedConfig.add(it)
        }

        previousChange = Triple(Pair(ChangeType.SAVE, name), serverChannel, serverChannel.rolePermissionOverwrites)
        // make sure to run this AFTER saving previous state
        permissions[name] = selectedConfig

        message.success("Saved current channel permissions, use `$fullName print $name` to print permissions!")
    }

    private suspend fun print(name: String, message: Message) {
        val s = StringBuilder()

        val selectedChannel = permissions[name] ?: run {
            message.error("Couldn't find `$name` in saved channel presets!")
            return
        }

        selectedChannel.forEach {
            s.append("${it.role.mention}\n" +
                    "Allow: ${it.allow.pretty()}\n" +
                    "Deny: ${it.deny.pretty()}\n\n")
        }

        permissions[name] = selectedChannel

        if (s.isEmpty()) {
            message.error("No saved permissions for `$name`!")
        } else message.normal(s.toString())
    }

    private suspend fun load(name: String, message: Message) {
        val selectedChannel = permissions[name] ?: run {
            message.error("Couldn't find `$name` in saved channel presets!")
            return
        }

        val serverChannel = message.serverChannel(message) ?: run { return }
        previousChange = Triple(Pair(ChangeType.LOAD, name), serverChannel, serverChannel.rolePermissionOverwrites)

        serverChannel.setPermissions(selectedChannel)

        message.success("Loaded channel permissions from `$name`!")
    }

    private suspend fun undo(message: Message) {
        val m = message.normal("Attempting to undo last change...")

        previousChange?.let {
            m.edit {
                description = "Attempting to undo last change...\nFound: ${it.second.name.toHumanReadable()}"
                color = Colors.PRIMARY.color
            }

            when (it.first.first) {
                ChangeType.SAVE -> {
                    permissions[it.first.second] = it.third.toHashSet()

                    m.edit {
                        description = "Attempting to undo last change...\n" +
                                "Found: ${it.second.name.toHumanReadable()}\n\n" +
                                "Unsaved, set ${it.first.second} to original permissions"
                        color = Colors.SUCCESS.color
                    }

                    previousChange = null
                }

                ChangeType.LOAD -> {
                    it.second.setPermissions(it.third.toHashSet())

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
            message.error("Channel category is null! Are you running this from a DM?")
            return
        }

        if (reverse) {
            category.setPermissions(perms.toHashSet())
            message.success("Synchronized category permissions to the `${serverChannel.name.toHumanReadable()}` channel!")
        } else {
            serverChannel.setPermissions(perms.toHashSet())
            message.success("Synchronized channel permissions to the `${category.name.toHumanReadable()}` category!")
        }
    }

    private suspend fun lockOrUnlock(category: Boolean, lock: Boolean, message: Message, server: Server?) {
        val s = server ?: run { message.error("Server is null, are you running this from a DM?"); return }
        val everyone = s.roles.find(s.id)!! // this cannot be null, as it's the @everyone role and we already checked server null

        val serverChannel = (if (category) message.serverChannel?.category else message.serverChannel)
            ?: run { message.error("${if (category) "Category" else "Server channel"} was null, was you running this from a DM?"); return }

        val perms: RolePermissionOverwrite?

        perms = if (lock) {
            message.success("Locked ${if (category) "category" else "channel"}!")
            RolePermissionOverwrite(everyone, PermissionSet(0), PermissionSet(2048))
        } else {
            message.success("Unlocked ${if (category) "category" else "channel"}!")
            RolePermissionOverwrite(everyone, PermissionSet(0), PermissionSet(0))
        }

        serverChannel.edit {
            rolePermissionOverwrites.add(perms)
        }

    }

    private suspend fun ServerChannel.setPermissions(permissions: HashSet<RolePermissionOverwrite>) {
        this.edit {
            this.rolePermissionOverwrites.removeAll(this.rolePermissionOverwrites)

            permissions.forEach {
                this.rolePermissionOverwrites.add(it)
            }
        }
    }

    private suspend fun Message.serverChannel(message: Message): ServerChannel? {
        val sc = this.server?.channels?.find(this.channel.id)

        if (sc == null) {
            message.error("Channel is null! Are you running this from a DM?")
        }

        return sc
    }

    /* <Saved Config, <Role ID, Pair<Allowed, Disallowed>>> */
    private val permissions = HashMap<String, HashSet<RolePermissionOverwrite>>()

    private var previousChange: Triple<Pair<ChangeType, String>, ServerChannel, Collection<RolePermissionOverwrite>>? = null

    private enum class ChangeType {
        SAVE, LOAD
    }
}
