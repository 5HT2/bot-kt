package helpers

import MojangUtils.insertDashes
import helpers.StringHelper.isUUID
import java.io.File
import java.net.URL
import java.net.URLEncoder

object StringHelper {
    fun String.isUrl() = Regex("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)").matches(this)

    fun String.toHumanReadable() = this.toLowerCase().replace("[_-]".toRegex(), " ").capitalizeWords()

    fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize() }

    fun String.trim(last: Int) = this.substring(0, this.length - last)

    fun String.flat(max: Int) = this.substring(0, this.length.coerceAtMost(max))

    fun String.readBytes() = URL(this).readBytes()

    fun String.uriEncode(): String = URLEncoder.encode(this, "utf-8")

    fun String.writeBytes(url: String): Int {
        val bytes = url.readBytes()
        File(this).writeBytes(bytes)
        return bytes.size
    }

    /** @throws NumberFormatException */
    fun String.toUserID() = this.replace("[<@!>]".toRegex(), "").toLong()

    fun String.isUUID() = Regex("[a-z0-9].{7}-[a-z0-9].{3}-[a-z0-9].{3}-[a-z0-9].{3}-[a-z0-9].{11}").matches(this)

    /** @return a properly formatted UUID, null if can't be formatted */
    fun String.fixedUUID(): String? {
        if (this.isUUID()) return this
        if (length < 32) return null
        val fixed = this.insertDashes()
        if (fixed.isUUID()) return fixed
        return null
    }
}
