package org.kamiblue.botkt.config

open class ServerConfig(name: String) : AbstractConfig(name) {

    final override val path: String
        get() = "configs/$server"

    var server = -1L; private set

    fun init(server: Long) {
        if (server != -1L) {
            throw IllegalStateException("Config $name is already initialized")
        }

        if (server > 0L) {
            throw IllegalArgumentException("$server is not a valid server id")
        }

        this.server = server
    }

}
