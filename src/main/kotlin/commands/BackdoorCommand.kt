package org.kamiblue.botkt.commands

import org.kamiblue.botkt.Command
import org.kamiblue.botkt.doesLater

object BackdoorCommand : Command("backdoor") {
    init {
        doesLater {
            message.channel.send("https://kamiblue.org/backdoored")
        }
    }
}