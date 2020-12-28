package org.kamiblue.botkt.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.from(ZoneOffset.UTC))

fun Instant.prettyFormat(): String = formatter.format(this)

fun Instant.untilNow(unit: ChronoUnit = ChronoUnit.DAYS) = this.until(Instant.now(), unit)

fun formatDuration(durationMillis: Long, displaySeconds: Boolean = true): String {
    val week = durationMillis / 604800000L
    val day = durationMillis / 86400000L % 7
    val hour = durationMillis / 3600000L % 24L
    val minute = durationMillis / 60000L % 60L
    val second = durationMillis / 1000L % 60L
    var append = false

    return StringBuilder(4).apply {
        if (append || week != 0L) {
            append(grammar(week, "week"))
            append = true
        }

        if (append || day != 0L) {
            append(grammar(day, "day"))
            append = true
        }

        if (append || hour != 0L) {
            append(grammar(hour, "hour"))
            append = true
        }

        if (append || minute != 0L) {
            append(grammar(minute, "minute"))
        }

        if (!append || displaySeconds) {
            append(grammar(second, "second", false))
        }
    }.toString()
}

private fun grammar(time: Long, unit: String, join: Boolean = true) =
    (if (time > 1 || time == 0L) "$time ${unit}s" else "$time $unit") + if (join) ", " else ""
