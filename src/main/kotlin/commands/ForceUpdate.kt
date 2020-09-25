package commands

import Bot
import Command
import doesLater

object ForceUpdate : Command("forceupdate"){
    init{
        doesLater{
            try{
                Bot().updateChannel()
            }catch(err: Exception){
                message.channel.send{
                    embed{
                        color = Colors.error
                        title = "Error! Something went wrong when executing this command!"
                        field("Stacktrace:", err.toString(), false)
                    }
                }
            }
        }
    }
}