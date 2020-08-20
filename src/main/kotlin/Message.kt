import net.ayataka.kordis.event.events.message.MessageReceiveEvent

object Respond {
    suspend fun reply(event: MessageReceiveEvent, reply: String) {
        event.message.channel.send(reply)
    }
}