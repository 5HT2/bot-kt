import kotlinx.coroutines.runBlocking
import net.ayataka.kordis.Kordis
import net.ayataka.kordis.event.EventHandler
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import net.ayataka.kordis.event.events.server.user.UserJoinEvent

fun main() = runBlocking {
    TestBot().start()
}

class TestBot {
    suspend fun start() {
        println("Starting bot!")
        val client = Kordis.create {
            token = "< insert your bot token here >"

            // Simple Event Handler
            addHandler<UserJoinEvent> {
                println(it.member.name + " has joined")
            }

            // Annotation based Event Listener
            addListener(this@TestBot)
        }
        println("Initialized bot!")
    }

    @EventHandler
    suspend fun onMessageReceive(event: MessageReceiveEvent) {
        // Simple ping-pong
        if (event.message.content.equals("!ping", true)) {
            event.message.channel.send("!pong")
        }

        // Sending an embedded message
/*
        if (event.message.content == "!serverinfo") {
            event.message.channel.send {
                embed {
                    author(name = server.name)
                    field("ID", server.id)
                    field("Server created", server.timestamp.formatAsDate(), true)
                    field("Members", server.members.joinToString { it.name }, true)
                    field("Text channels", server.textChannels.joinToString { it.name })
                    field("Voice channels", server.voiceChannels.joinToString { it.name }.ifEmpty { "None" })
                    field("Emojis", server.emojis.size, true)
                    field("Roles", server.roles.joinToString { it.name }, true)
                    field("Owner", server.owner!!.mention, true)
                    field("Region", server.region.displayName, true)
                }
            }
        }
*/

        // Adding a role
        if (event.message.content.equals("!member", true)) {
            val server = event.server ?: return
            val member = event.message.member ?: return

            server.roles.findByName("Member", true)?.let {
                member.addRole(it)
            }
        }
    }
}
