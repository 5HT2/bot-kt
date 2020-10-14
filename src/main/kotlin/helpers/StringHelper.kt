package helpers

import net.ayataka.kordis.entity.server.Server
import java.io.File
import java.net.URL

object StringHelper {
    fun formattedRole(id: Long?, server: Server?) = if (id == server?.id) "@everyone" else id?.let { "<@&${server?.roles?.find(it)?.id}>" } ?: "$id"

    fun String.isUrl() = Regex("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)").matches(this)

    fun String.toHumanReadable() = this.toLowerCase().replace("[_-]".toRegex(), " ").capitalizeWords()

    fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize() }

    fun String.trim(last: Int) = this.substring(0, this.length - last)

    fun String.flat(max: Int) = this.substring(0, this.length.coerceAtMost(max))

    fun String.downloadBytes() = URL(this).readBytes()

    fun String.writeBytes(url: String): Int {
        val bytes = url.downloadBytes()
        File(this).writeBytes(bytes)
        return bytes.size
    }
}
