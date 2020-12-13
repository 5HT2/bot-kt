package org.kamiblue.botkt.event

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.kamiblue.event.eventbus.AbstractAsyncEventBus
import org.kamiblue.event.listener.AsyncListener
import org.kamiblue.event.listener.Listener
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object BotEventBus : AbstractAsyncEventBus() {
    override val subscribedObjects = ConcurrentHashMap<Any, MutableSet<Listener<*>>>()
    override val subscribedListeners = ConcurrentHashMap<Class<*>, MutableSet<Listener<*>>>()
    override val newSet get() = ConcurrentSkipListSet<Listener<*>>(Comparator.reverseOrder())

    override val subscribedObjectsAsync = ConcurrentHashMap<Any, MutableSet<AsyncListener<*>>>()
    override val subscribedListenersAsync = ConcurrentHashMap<Class<*>, MutableSet<AsyncListener<*>>>()
    override val newSetAsync get() = ConcurrentSkipListSet<AsyncListener<*>>(Comparator.reverseOrder())

    override fun post(event: Any) {
        runBlocking {
            val deferredList = subscribedListenersAsync[event.javaClass]?.map {
                async {
                    @Suppress("UNCHECKED_CAST") // IDE meme
                    (it as AsyncListener<Any>).function.invoke(event)
                }
            }
            super.post(event)
            deferredList?.awaitAll()
        }
    }
}