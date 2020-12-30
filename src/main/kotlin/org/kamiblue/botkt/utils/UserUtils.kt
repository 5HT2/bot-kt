package org.kamiblue.botkt.utils

import net.ayataka.kordis.entity.findByTag
import net.ayataka.kordis.entity.user.User
import org.kamiblue.botkt.Main
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.StringUtils.toUserID
import java.time.temporal.ChronoUnit

fun User.accountAge(unit: ChronoUnit = ChronoUnit.DAYS): Long {
    return timestamp.untilNow(unit)
}

suspend fun MessageExecuteEvent.findUserEverywhere(username: String): User? {
    val members = message.server?.members
    val id = username.toUserID()

    return id?.let { members?.find(it) }
        ?: members?.findByTag(username, true)
        ?: members?.findByName(username, true)
        ?: id?.let { Main.client.getUser(it) }
        ?: run {
            channel.error("Couldn't find user nor a valid ID!")
            null
        }
}
