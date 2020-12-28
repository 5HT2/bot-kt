package org.kamiblue.botkt.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.from(ZoneOffset.UTC))

fun Instant.prettyFormat(): String = formatter.format(this)

fun Instant.untilNow(unit: ChronoUnit = ChronoUnit.DAYS) = this.until(Instant.now(), unit)

fun formatDuration(durationMillis: Long): String {
    val day = durationMillis / 86400000L
    val hour = durationMillis / 3600000L % 24L
    val minute = durationMillis / 60000L % 60L
    val second = durationMillis / 1000L % 60L

    return StringBuilder(4).apply {
        var added = false

        if (added || day != 0L) {
            append(grammar(day, "day"))
            added = true
        }

        if (added || hour != 0L) {
            append(grammar(hour, "hour"))
            added = true
        }

        if (added || minute != 0L) {
            append(grammar(minute, "minute"))
        }

        append(grammar(second, "second", false))
    }.toString()
}

private fun grammar(long: Long, string: String, appendSpace: Boolean = true) =
    (if (long > 1 || long == 0L) "$long ${string}s" else "$long $string") + if (appendSpace) " " else ""
