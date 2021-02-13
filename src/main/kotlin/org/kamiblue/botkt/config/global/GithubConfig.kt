package org.kamiblue.botkt.config.global

import org.kamiblue.botkt.config.GlobalConfig

object GithubConfig : GlobalConfig("System") {
    /**
     * Generated with the full repo access at https://github.com/settings/tokens.
     */
    val githubToken by setting("Github Token", "", "Generated with the full repo access at https://github.com/settings/tokens.", true)
}
