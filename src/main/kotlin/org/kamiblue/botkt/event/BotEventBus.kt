package org.kamiblue.botkt.event

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    private val dispatcher = CoroutineScope(Dispatchers.Default + CoroutineName("BotEventBus"))

    override fun post(event: Any) {
        subscribedListenersAsync[event.javaClass]?.forEach {
            dispatcher.launch {
                try {
                    @Suppress("UNCHECKED_CAST") // IDE meme
                    (it as AsyncListener<Any>).function.invoke(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        subscribedListeners[event.javaClass]?.forEach {
            try {
                @Suppress("UNCHECKED_CAST") // IDE meme
                (it as Listener<Any>).function.invoke(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}