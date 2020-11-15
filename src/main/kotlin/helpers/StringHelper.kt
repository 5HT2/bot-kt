package helpers

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
}
