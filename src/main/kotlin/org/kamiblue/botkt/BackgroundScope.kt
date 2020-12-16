package org.kamiblue.botkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newFixedThreadPoolContext
import net.ayataka.kordis.utils.TimerScope
import net.ayataka.kordis.utils.timer

@Suppress("EXPERIMENTAL_API_USAGE")
object BackgroundScope : CoroutineScope by CoroutineScope(newFixedThreadPoolContext(2, "bot-kt Background")) {

    private val list = ArrayList<Triple<Long, String?, suspend TimerScope.() -> Unit>>()
    private var started = false

    fun start() {
        started = true
        for ((delay, errorMessage, block) in list) {
            launch(delay, errorMessage, block)
        }
    }

    fun add(delay: Long, errorMessage: String? = null, block: suspend TimerScope.() -> Unit) {
        if (!started) {
            list.add(Triple(delay, errorMessage, block))
        } else {
            launch(delay, errorMessage, block)
        }
    }

    private fun launch(delay: Long, errorMessage: String? = null, block: suspend TimerScope.() -> Unit) {
        timer(delay, false) {
            try {
                block()
            } catch (e: Exception) {
                Main.logger.warn(errorMessage ?: "Exception in ${Thread.currentThread().name}", e)
            }
        }
    }

}
