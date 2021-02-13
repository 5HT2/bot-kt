package org.kamiblue.botkt.config.global

import org.kamiblue.botkt.config.GlobalConfig

object GithubConfig : GlobalConfig("System") {
    val githubToken by setting("Github Token", "", "Generated with the full repo access at https://github.com/settings/tokens.", true)
    val defaultGithubUser by setting("Default Github User", "", "Default user/org being used in the IssueCommand")
}
