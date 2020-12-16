package org.kamiblue.botkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newFixedThreadPoolContext
import net.ayataka.kordis.utils.TimerScope
import net.ayataka.kordis.utils.timer

@Suppress("EXPERIMENTAL_API_USAGE")
object BackgroundScope : CoroutineScope by CoroutineScope(newFixedThreadPoolContext(2, "Bot-kt Background")) {

    fun launch(delay: Long, errorMessage: String? = null, block: suspend TimerScope.() -> Unit) {
        timer(delay) {
            try {
                block()
            } catch (e: Exception) {
                Main.logger.warn(errorMessage ?: "Exception in ${Thread.currentThread().name}", e)
            }
        }
    }

}