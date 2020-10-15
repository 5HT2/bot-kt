package commands

import Colors
import Command
import PermissionTypes.MANAGE_CHANNELS
import Send.error
import Send.normal
import Send.success
import arg
import commands.ChannelCommand.ChangeType.LOAD
import commands.ChannelCommand.ChangeType.SAVE
import doesLaterIfHas
import helpers.StringHelper.formattedRole
import helpers.StringHelper.toHumanReadable
import literal
import net.ayataka.kordis.entity.edit
import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.channel.ServerChannel
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.permission.overwrite.RolePermissionOverwrite
import string

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
            literal("reverse") {
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
    }

    private suspend fun save(name: String, serverChannel: ServerChannel, message: Message) {
        val selectedConfig = HashSet<RolePermissionOverwrite>()

        serverChannel.rolePermissionOverwrites.forEach {
            selectedConfig.add(it)
        }

        previousChange = Triple(Pair(SAVE, name), serverChannel, serverChannel.rolePermissionOverwrites)
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
            s.append("${formattedRole(it.role.id, message.server)}\n" +
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
        previousChange = Triple(Pair(LOAD, name), serverChannel, serverChannel.rolePermissionOverwrites)

        serverChannel.setPermissions(selectedChannel)

        message.success("Loaded channel permissions from `$name`!")
    }

    private suspend fun undo(message: Message) {
        val m = message.normal("Attempting to undo last change...")

        previousChange?.let {
            m.edit {
                description = "Attempting to undo last change...\nFound: ${it.second.name.toHumanReadable()}"
                color = Colors.primary
            }

            when (it.first.first) {
                SAVE -> {
                    permissions[it.first.second] = it.third.toHashSet()

                    m.edit {
                        description = "Attempting to undo last change...\n" +
                                "Found: ${it.second.name.toHumanReadable()}\n\n" +
                                "Unsaved, set ${it.first.second} to original permissions"
                        color = Colors.success
                    }

                    previousChange = null
                }

                LOAD -> {
                    it.second.setPermissions(it.third.toHashSet())

                    m.edit {
                        description = "Attempting to undo last change...\n" +
                                "Found: ${it.second.name.toHumanReadable()}\n\n" +
                                "Unloaded, set ${it.first.second} to original permissions"
                        color = Colors.success
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

    private suspend fun ServerChannel.setPermissions(permissions: HashSet<RolePermissionOverwrite>) {
        this.edit {
            this.rolePermissionOverwrites.removeAll(this.rolePermissionOverwrites)

            permissions.forEach {
                this.rolePermissionOverwrites.add(it)
            }
        }
    }

    private fun PermissionSet.pretty(): String {
        val prettified = this.joinToString { it.name.toHumanReadable() }
        return if (prettified.isEmpty()) "None" else prettified
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
