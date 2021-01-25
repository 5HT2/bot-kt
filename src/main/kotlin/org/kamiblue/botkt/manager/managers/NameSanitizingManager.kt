package org.kamiblue.botkt.manager.managers

import net.ayataka.kordis.entity.server.member.Member
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import net.ayataka.kordis.event.events.server.user.UserJoinEvent
import net.ayataka.kordis.event.events.server.user.UserUpdateEvent
import net.ayataka.kordis.exception.MissingPermissionsException
import org.kamiblue.botkt.ConfigType
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.NameSanitizingConfig
import org.kamiblue.botkt.manager.Manager
import org.kamiblue.commons.extension.max
import org.kamiblue.event.listener.asyncListener
import kotlin.text.StringBuilder

object NameSanitizingManager : Manager {

    private val config get() = ConfigManager.readConfigSafe<NameSanitizingConfig>(ConfigType.NAME_SANITIZING, false)
    private val checkedNicks = hashMapOf<Long, Long>()

    init {
        asyncListener<MessageReceiveEvent> {
            val member = it.message.member ?: return@asyncListener
            if (timeCheck(member)) return@asyncListener

            fixNickName(member)
        }

        asyncListener<UserJoinEvent> {
            fixNickName(it.member)
        }

        asyncListener<UserUpdateEvent> {
            fixNickName(it.member)
        }
    }

    private fun timeCheck(member: Member): Boolean {
        val config = config ?: return true

        synchronized(checkedNicks) {
            val currentTime = System.currentTimeMillis()
            checkedNicks[member.id]?.let { lastTime ->
                if (currentTime - lastTime < config.talkNameLimit * 1000) return true
            }

            checkedNicks[member.id] = currentTime
            return false
        }
    }

    private suspend fun fixNickName(member: Member) {
        val config = config ?: return

        if (config.ignoreRoles?.isNotEmpty() == true && member.roles.any { config.ignoreRoles.contains(it.id) }) {
            return
        }

        var nickname = member.nickname ?: member.name
        val originalName = nickname

        // If the nickname has any disallowed words in it, reset entirely, don't keep original
        if (config.disallowedRegexes.any { it.containsMatchIn(nickname) }) {
            nickname = getAllowedWords("") ?: return
            member.setNickSafe(nickname)
            return
        }

        // Try to remove disallowed prefixes
        if (config.removePrefix) {
            while (nickname.isNotEmpty() && !config.allowedChars.contains(nickname.first())) {
                nickname = nickname.substring(1)
            }
        }

        val validChars = nickname.count { config.allowedChars.contains(it) }

        // If the last check made this too short, this will fix it
        if (validChars < config.minNormalChars) {
            nickname = getAllowedWords(nickname) ?: return
        }

        // Make sure there's enough allowed chars
        if ((validChars / nickname.length.toDouble()) * 100 < config.minNormalPercentage) {
            nickname = getAllowedWords(nickname) ?: return
        }

        if (nickname == originalName) return
        member.setNickSafe(nickname)
    }

    private fun getAllowedWords(append: String): String? {
        val config = config ?: return null
        if (config.wordList.isEmpty()) return null

        return StringBuilder().apply {
            for (i in 0 until config.wordListAmount) {
                append(config.wordList.random() + " ")
            }
            append(append)
        }.toString()
    }

    private suspend fun Member.setNickSafe(nickname: String) {
        try {
            this.setNickname(nickname.max(32))
            Main.logger.debug("Set $tag / $id nickname to '$nickname'")
        } catch (e: MissingPermissionsException) {
            // this is fine
        }
    }
}
