package org.kamiblue.botkt.config

open class ServerConfig(name: String) : AbstractConfig(name) {

    final override val path: String
        get() = "configs/$server"

    var server = -1L; private set

    fun init(server: Long) {
        this.server = server
    }

}
