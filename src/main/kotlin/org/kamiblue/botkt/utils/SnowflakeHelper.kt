package org.kamiblue.botkt.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object SnowflakeHelper {
    /**
     * @return an Epoch millisecond from a Discord Snowflake
     * [offset] defaults to the first millisecond of 2015, or a "Discord Epoch"
     */
    fun Long.fromDiscordSnowFlake(offset: Long = 1420070400000): Long {
        return (this shr 22) + offset
    }

    /**
     * @return an [Instant] from [epoch]
     */
    fun Long.toInstant(epoch: Long = this.fromDiscordSnowFlake()): Instant {
        return Instant.ofEpochMilli(epoch)
    }

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC))
    private val replaceRegex = Regex("\\..*")

    /**
     * @return a "pretty" format lazily based off of ISO8601. Looks like "hh:mm:ss yyyy-mm-dd"
     */
    fun Instant.prettyFormat(): String {
        val split = formatter.format(this).split('T')
        return split[1].replace(replaceRegex, " ") + split[0]
    }
}