import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ayataka.kordis.event.events.message.MessageReceiveEvent
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author dominikaaaa
 * @since 2020/08/18 16:30
 */
open class Command(_name: String) : LiteralArgumentBuilder<Cmd>(_name) {
    val name = _name
}

class Cmd(val event: MessageReceiveEvent) {

    private var asyncQueue: ConcurrentLinkedQueue<suspend MessageReceiveEvent.() -> Unit> = ConcurrentLinkedQueue()

    infix fun later(block: suspend MessageReceiveEvent.() -> Unit) {
        asyncQueue.add(block)
    }

    suspend fun file(event: MessageReceiveEvent) = coroutineScope {
        asyncQueue.map {
            async {
                event.it()
            }
        }.awaitAll()
    }
}