package org.kamiblue.botkt.plugin

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.kamiblue.botkt.Main
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.Type
import java.net.URLClassLoader
import java.security.MessageDigest

class PluginLoader(
    private val file: File
) {

    private val url = file.toURI().toURL()
    private val loader = URLClassLoader(arrayOf(url), this.javaClass.classLoader)
    private val mainClassPath: String

    init {
        val stream = loader.getResourceAsStream("plugin.info")
            ?: throw FileNotFoundException("plugin.info is not found under jar ${file.name}")
        mainClassPath = stream.reader().readText()
    }

    fun verify(): Boolean {
        val bytes = file.inputStream().use {
            it.readBytes()
        }

        val result = StringBuilder().run {
            sha256.digest(bytes).forEach {
                append(String.format("%02x", it))
            }
            toString()
        }

        Main.logger.info("SHA-256 checksum for ${file.name}: $result")

        return checksumSets.contains(result)
    }

    fun load(): Plugin {
        val clazz = Class.forName(mainClassPath, true, loader)
        return clazz.newInstance() as Plugin
    }

    private companion object {
        val sha256: MessageDigest = MessageDigest.getInstance("SHA-256")
        val type: Type = object : TypeToken<HashSet<String>>() {}.type
        val checksumSets: HashSet<String> = Gson().fromJson(File("verify.json").bufferedReader(), type)
    }

}