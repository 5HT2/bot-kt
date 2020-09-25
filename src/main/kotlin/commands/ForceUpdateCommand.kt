package commands

import Bot
import Command
import doesLater

object ForceUpdateCommand : Command("forceupdate"){
    init{
        doesLater{
            try{
                // TODO: Check member permission
                if( message.author!!.id != 563138570953687061){ return@doesLater }
                message.channel.send {
                    embed {
                        color = Colors.success
                        title = "Update success! Check the voice channel."
                    }
                }
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