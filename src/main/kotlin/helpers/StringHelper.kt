package helpers

import java.io.File
import java.net.URL

object StringHelper {
    fun String.isUrl(): Boolean {
        return Regex("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)").matches(
            this
        )
    }

    fun String.toHumanReadable() = this.toLowerCase().replace("[_-]".toRegex(), " ").capitalize()

    fun String.normalizeCase(): String {
        return this.substring(0, 1) + this.substring(1).toLowerCase()
    }

    fun String.trim(last: Int): String {
        return this.substring(0, this.length - last)
    }

    fun String.flat(max: Int): String {
        return this.substring(0, this.length.coerceAtMost(max))
    }

    fun String.downloadBytes(): ByteArray {
        return URL(this).readBytes()
    }

    fun String.writeBytes(url: String): Int {
        val bytes = url.downloadBytes()
        File(this).writeBytes(bytes)
        return bytes.size
    }
}
