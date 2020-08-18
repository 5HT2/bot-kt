import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.ayataka.kordis.event.events.message.MessageReceiveEvent

/**
 * @author dominikaaaa
 * @since 2020/08/18 16:30
 */
open class Command(_name: String) : LiteralArgumentBuilder<Cmd>(_name) {
    val name = _name
}

class Cmd(_event: MessageReceiveEvent) {
    val event = _event
}