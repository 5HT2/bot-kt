package org.kamiblue.botkt.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.from(ZoneOffset.UTC))

fun Instant.prettyFormat(): String = formatter.format(this)

fun Instant.untilNow(unit: ChronoUnit = ChronoUnit.DAYS) = this.until(Instant.now(), unit)
