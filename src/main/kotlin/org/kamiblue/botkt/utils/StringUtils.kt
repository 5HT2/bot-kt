package org.kamiblue.botkt.utils

import java.net.URLEncoder

object StringUtils {
    private val urlRegex = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)".toRegex()
    private val humanReadableRegex = "[_-]".toRegex()

    fun String.isUrl() = urlRegex.matches(this)

    fun String.toHumanReadable() = toLowerCase().replace(humanReadableRegex, " ").capitalizeWords()

    fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize() }

    fun String.urlEncode(): String = URLEncoder.encode(this, "utf-8")

    fun String.toUserID() = replace("[<@!>]".toRegex(), "").toLongOrNull()

    fun String.elseEmpty(alternate: String) = if (isEmpty()) alternate else this

    fun <E> Iterable<E>.joinToChunks(
        separator: CharSequence = ", ",
        chunkSize: Int,
        lineTransformer: (E) -> String = { it.toString() }
    ): List<String> {
        val fields = ArrayList<String>()
        val stringBuilder = StringBuilder(chunkSize)

        forEach {
            val line = "${lineTransformer(it)}$separator"

            if (stringBuilder.length + line.length < 1024) {
                stringBuilder.append(line)
            } else {
                fields.add(stringBuilder.toString())
                stringBuilder.clear()
            }
        }

        if (stringBuilder.isNotEmpty()) fields.add(stringBuilder.toString())

        return fields
    }

}
