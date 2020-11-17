package org.kamiblue.botkt.utils

import java.io.File
import java.net.URL
import java.net.URLEncoder

@Suppress("UNUSED")
object StringUtils {
    private val urlRegex = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)".toRegex()
    private val humanReadableRegex = "[_-]".toRegex()

    fun String.isUrl() = urlRegex.matches(this)

    fun String.toHumanReadable() = this.toLowerCase().replace(humanReadableRegex, " ").capitalizeWords()

    fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize() }

    fun String.trim(last: Int) = this.substring(0, this.length - last)

    fun String.flat(max: Int) = this.substring(0, this.length.coerceAtMost(max))

    fun String.readBytes() = URL(this).readBytes()

    fun String.uriEncode(): String = URLEncoder.encode(this, "utf-8")

    fun String.firstInSentence() = this.split(" ").firstOrNull() ?: this

    fun String.writeBytes(url: String): Int {
        val bytes = url.readBytes()
        File(this).writeBytes(bytes)
        return bytes.size
    }

    fun String.toUserID() = this.replace("[<@!>]".toRegex(), "").toLongOrNull()
}
