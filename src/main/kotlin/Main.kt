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
        val config = FileManager.readConfig<AuthConfig>(ConfigType.AUTH, false)

        if (config?.botToken == null) {
            println("Bot token not found, make sure your file is formatted correctly!. \nExiting...")
            return
        }

        val client = Kordis.create {
            token = config.botToken

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
        if (event.message.content.equals(";ping", true)) {
            event.message.channel.send("Pong! Latency is 115ms. API Latency is 72ms\n")
        }

        val server = event.server!!
        // Sending an embedded message
        if (event.message.content == ";serverinfo") {
//            event.message.channel.send("${server.name} ${server.id} ${server.emojis.size}")
            event.message.channel.send {
                embed {
                    author(name = server.name)
                    field("ID", server.id)
                    field("Server created", Converter.epochToDate(server.timestamp.epochSecond).toString(), true)
//                    field("Members", server.members.joinToString { it.name }, true)
//                    field("Text channels", server.textChannels.joinToString { it.name })
//                    field("Voice channels", server.voiceChannels.joinToString { it.name }.ifEmpty { "None" })
//                    field("Emojis", server.emojis.size, true)
//                    field("Roles", server.roles.joinToString { "<@&${it.id}>" }, true)
//                    field("Owner", server.owner!!.mention, true)
//                    field("Region", server.region.displayName, true)
                }
            }
        }

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
