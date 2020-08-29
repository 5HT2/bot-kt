package commands

import Command
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
