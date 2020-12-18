package org.kamiblue.botkt.utils

import java.net.URLEncoder

@Suppress("UNUSED")
object StringUtils {
    private val urlRegex = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)".toRegex()
    private val humanReadableRegex = "[_-]".toRegex()

    fun String.isUrl() = urlRegex.matches(this)

    fun String.isCdnUrl() = urlRegex.matches(this)

    fun String.toHumanReadable() = this.toLowerCase().replace(humanReadableRegex, " ").capitalizeWords()

    fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize() }

    fun String.trim(last: Int) = this.substring(0, this.length - last)

    fun String.urlEncode(): String = URLEncoder.encode(this, "utf-8")

    fun String.firstInSentence() = this.split(" ").firstOrNull() ?: this

    fun String.toUserID() = this.replace("[<@!>]".toRegex(), "").toLongOrNull()
}
