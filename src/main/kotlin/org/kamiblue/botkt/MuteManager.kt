package org.kamiblue.botkt

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import net.ayataka.kordis.entity.botUser
import net.ayataka.kordis.entity.everyone
import net.ayataka.kordis.entity.server.Server
import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.entity.server.permission.PermissionSet
import net.ayataka.kordis.entity.server.role.Role
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.server.user.UserJoinEvent
import net.ayataka.kordis.event.events.server.user.UserRoleUpdateEvent
import net.ayataka.kordis.utils.timer
import org.kamiblue.botkt.utils.Colors
import java.io.*
import java.util.concurrent.ConcurrentHashMap

object MuteManager {

    val serverMap = HashMap<Long, ServerMuteInfo>() // <Server ID, ServerMuteInfo>
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object : TypeToken<LinkedHashMap<Long, Map<Long, Long>>>() {}.type
    private val muteFile = File("config/mute.json")
    private val muteManagerScope = CoroutineScope(Dispatchers.Default)

    fun save() {
        BufferedWriter(FileWriter(muteFile)).use {
            val cacheMap = LinkedHashMap<Long, Map<Long, Long>>()

            for ((id, serverMuteInfo) in serverMap) {
                cacheMap[id] = serverMuteInfo.muteMap
            }

            gson.toJson(cacheMap, it)
        }
    }

    fun load() {
        if (!muteFile.exists()) {
            save()
            return
        }

        BufferedReader(FileReader(muteFile)).use { reader ->
            val cacheMap = gson.fromJson<LinkedHashMap<Long, Map<Long, Long>>>(reader, type)
            for ((id, cacheMuteMap) in cacheMap) {
                serverMap.getOrPut(id) {
                    ServerMuteInfo(Main.client.servers.find(id)!!)
                }.apply {
                    muteMap.clear()
                    muteMap.putAll(cacheMuteMap)
                }
            }
        }
    }

    @Suppress("UNUSED")
    @EventHandler
    suspend fun userJoinedListener(event: UserJoinEvent) {
        reAdd(event.member)
    }

    @Suppress("UNUSED")
    @EventHandler
    suspend fun onUserUpdateRoles(event: UserRoleUpdateEvent) {
        val mutedRole = serverMap[event.server.id]?.getMutedRole() ?: return
        if (event.before.contains(mutedRole) && !event.member.roles.contains(mutedRole)) {
            reAdd(event.member)
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
        val muteMap = ConcurrentHashMap<Long, Long>() // <Member ID, Unmute Time>
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
            coroutineMap[member.id] = muteManagerScope.launch {
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
            muteManagerScope.launch {
                delay(5000L)
                while (isActive) {
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

    init {
        muteManagerScope.timer(30000L) {
            try {
                delay(30000L)
                save()
            } catch (e: Exception) {
                // this is fine
            }
        }

        Main.client.addListener(this)
    }

}