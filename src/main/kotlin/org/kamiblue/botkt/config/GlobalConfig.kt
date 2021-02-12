package org.kamiblue.botkt.config

open class GlobalConfig(name: String) : AbstractConfig(name) {
    final override val path: String
        get() = "configs"
}
