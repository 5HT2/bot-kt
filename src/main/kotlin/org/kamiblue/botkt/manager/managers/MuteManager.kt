package org.kamiblue.botkt.manager.managers

import kotlinx.coroutines.*
import net.ayataka.kordis.entity.botUser
import net.ayataka.kordis.entity.everyone
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.role.Role
import net.ayataka.kordis.event.events.server.user.UserJoinEvent
import net.ayataka.kordis.event.events.server.user.UserRoleUpdateEvent
import org.kamiblue.botkt.BackgroundScope
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.config.ServerConfigs
import org.kamiblue.botkt.config.server.MuteConfig
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.botkt.utils.Colors
import org.kamiblue.event.listener.asyncListener
import java.io.*

object MuteManager : Manager {

    val serverMap = HashMap<Long, ServerMuteInfo>() // <Server ID, ServerMuteInfo>

    init {
        asyncListener<UserJoinEvent> {
            reAdd(it.member)
        }

        asyncListener<UserRoleUpdateEvent> {
            val mutedRole = serverMap[it.server.id]?.getMutedRole() ?: return@asyncListener
            if (it.before.contains(mutedRole) && !it.member.roles.contains(mutedRole)) {
                reAdd(it.member)
            }
        }
    }

    private suspend fun reAdd(member: Member) {
        serverMap[member.server.id]?.let { serverMuteInfo ->
            serverMuteInfo.muteMap[member.id]?.let {
                val mutedRole = serverMuteInfo.getMutedRole()
                val duration = it - System.currentTimeMillis()

                if (duration > 1000L) {
                    try {
                        member.addRole(mutedRole)
                    } catch (e: Exception) {
                        // Ignore it
                    }
                } else {
                    serverMuteInfo.muteMap.remove(member.id)
                }
            }
        }
    }

    class ServerMuteInfo(val server: Server) {
        private val config = ServerConfigs.get<MuteConfig>(server)

        val muteMap get() = config.muteMap
        val coroutineMap = HashMap<Long, Job>() // <Member ID, Coroutine Job>

        private var mutedRole: Role? = null

        suspend fun getMutedRole() = mutedRole
            ?: server.roles.findByName("Muted", true)
            ?: server.createRole {
                name = "Muted"
                permissions = PermissionSet(server.roles.everyone.permissions.compile() and 68224001)
                position = server.members.botUser.roles.map { it.position }.maxOrNull()!!
            }

        suspend fun startUnmuteCoroutine(
            member: Member,
            role: Role,
            duration: Long
        ) {
            coroutineMap[member.id] = BackgroundScope.launch {
                delay(duration)

                try {
                    member.removeRole(role)
                    val bot = Main.client.botUser
                    member.getPrivateChannel().send {
                        embed {
                            field(
                                "You were unmuted automatically by:",
                                bot.mention
                            )
                            field(
                                "In the guild:",
                                server.name
                            )
                            color = Colors.SUCCESS.color
                            footer("ID: ${bot.id}", bot.avatar.url)
                        }
                    }
                } catch (e: Exception) {
                    // this is fine
                }

                muteMap.remove(member.id)
                coroutineMap.remove(member.id)
            }
        }

        init {
            BackgroundScope.launchLooping("Mute manager", 5000L) {
                for ((id, unmuteTime) in muteMap) {
                    delay(500L)
                    if (muteMap.containsKey(id) && !coroutineMap.containsKey(id)) {
                        val duration = unmuteTime - System.currentTimeMillis()
                        val member = server.members.find(id) ?: continue
                        startUnmuteCoroutine(member, getMutedRole(), duration)
                    }
                }
            }
        }
    }
}
