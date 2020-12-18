package org.kamiblue.botkt.utils

import net.ayataka.kordis.entity.user.User
import java.time.temporal.ChronoUnit

fun User.accountAge(unit: ChronoUnit = ChronoUnit.DAYS): Long {
    return timestamp.untilNow(unit)
}