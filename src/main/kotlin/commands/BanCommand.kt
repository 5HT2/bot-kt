package commands

import Command
import ConfigManager.readConfigSafe
import UserConfig
import long
import integer
import doesLater
import arg

object BanCommand : Command("ec") {
    init {
        long("user") {
            integer("deletemessagedays") {
                doesLater { context ->
                    val id: Long = context arg "user"
                    val messageDays: Int = context arg "deletemessagedays"
                    val user = Main.client!!.getUser(id)
                    val server = readConfigSafe<UserConfig>(
                        ConfigType.USER,
                        false
                    )?.primaryServerId?.let { Main.client?.servers?.find(it) } ?: run {
                        return@doesLater
                    }
                    user!!.ban(server, messageDays)
                }
            }
        }
    }
}