package org.kamiblue.botkt

import kotlinx.coroutines.*
import net.ayataka.kordis.entity.botUser
import net.ayataka.kordis.entity.everyone
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.role.Role
import java.util.concurrent.ConcurrentHashMap

object MuteManager {

    val serverMap = HashMap<Long, ServerMuteInfo>() // <Server ID, ServerMuteInfo>

    class ServerMuteInfo(val server: Server) {
        val muteMap = ConcurrentHashMap<Long, Long>() // <Member ID, Unmute Time>
        val coroutineMap = HashMap<Long, Job>() // <Member ID, Coroutine Job>

        private var mutedRole: Role? = null

        suspend fun getMutedRole() = mutedRole
            ?: server.roles.findByName("Muted")
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
            coroutineMap[member.id] = GlobalScope.launch {
                delay(duration)
                member.removeRole(role)
                muteMap.remove(member.id)
                coroutineMap.remove(member.id)
            }
        }

        init {
            GlobalScope.launch {
                delay(10000L)
                while (isActive) {
                    for ((id, unmuteTime) in muteMap) {
                        delay(500L)
                        if (!coroutineMap.contains(id)) {
                            val member = server.members.find(id) ?: continue
                            val duration = unmuteTime - System.currentTimeMillis()
                            startUnmuteCoroutine(member, getMutedRole(), duration)
                        }
                    }
                }
            }
        }
    }
}