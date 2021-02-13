package org.kamiblue.botkt.config.global

import org.kamiblue.botkt.config.GlobalConfig

// TODO: Get the github download counter out of this and make this server dependent
object CounterConfig : GlobalConfig("Counter") {
    val memberCounter by setting("Member Counter", true, "Enables member counter")
    val memberChannel by setting("Member Channel", -1L, "ID of the voice channel for the member counter")

    val downloadCounter by setting("Download Counter", true)
    val totalDownloadChannel by setting("Total Download Channel", -1L, "ID of the voice channel for total downloads")
    val nightlyDownloadChannel by setting("Latest Download Channel", -1L, "ID of the voice channel for latest downloads")

    val stableRepo by setting("Stable Url", "", "Main/stable repository for download counter")
    val nightlyRepo by setting("Nightly Url", "", "Beta/nightly repository for download counter")

    val perPage by setting("Per Page", 200, "The max releases per page when using the Github API")
}
