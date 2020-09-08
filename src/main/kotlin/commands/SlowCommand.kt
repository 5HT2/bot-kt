package commands

import Command
import StringHelper.MessageTypes.MISSING_PERMISSIONS
import arg
import doesLater
import integer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SlowCommand : Command("slow") {
    init {
        integer("wait") {
            integer("time") {
                doesLater { context ->
                    if (!Permissions.hasPermission(message, PermissionTypes.COUNCIL_MEMBER)) {
                        StringHelper.sendMessage(message.channel, MISSING_PERMISSIONS)
                        return@doesLater
                    }
                    val wait: Int = context arg "wait"
                    val time: Int = context arg "time"
                    val originalWait = message.serverChannel!!.rateLimitPerUser
                    message.serverChannel!!.edit {
                        rateLimitPerUser = wait
                    }
                    GlobalScope.launch {
                        delay(time * 1000L)
                        message.serverChannel!!.edit {
                            rateLimitPerUser = originalWait
                        }
                    }
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Enables for this channel for an amount of time." +
                "`;$name <wait> <time>`"
    }
}
