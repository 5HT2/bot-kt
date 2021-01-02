package org.kamiblue.botkt

import net.ayataka.kordis.utils.TimerScope

class BackgroundJob(
    val name: String,
    val delay: Long,
    val block: suspend TimerScope.() -> Unit
) {

    override fun equals(other: Any?) = this === other || (
        other is BackgroundJob &&
            name == other.name &&
            delay == other.delay
        )

    override fun hashCode() = 31 * name.hashCode() + delay.hashCode()
}
