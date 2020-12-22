package org.kamiblue.botkt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.newFixedThreadPoolContext
import net.ayataka.kordis.utils.TimerScope
import net.ayataka.kordis.utils.timer

@Suppress("EXPERIMENTAL_API_USAGE")
internal object BackgroundScope : CoroutineScope by CoroutineScope(newFixedThreadPoolContext(2, "Bot-kt Background")) {

    private val jobs = LinkedHashMap<BackgroundJob, Job?>()
    private var started = false

    fun start() {
        started = true
        for ((job, _) in jobs) {
            jobs[job] = startJob(job)
        }
    }

    fun launchLooping(name: String, delay: Long, block: suspend TimerScope.() -> Unit) {
        val job = BackgroundJob(name, delay, block)
        if (!started) {
            jobs[job] = null
        } else {
            startJob(job)
        }
    }

    fun cancel(job: BackgroundJob) = jobs.remove(job)?.cancel()

    private fun startJob(job: BackgroundJob): Job {
        return timer(job.delay, false) {
            try {
                job.block(this)
            } catch (e: Exception) {
                Main.logger.warn("Error occurred while running background job ${job.name}", e)
            }
        }
    }

}