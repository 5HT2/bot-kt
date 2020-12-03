package org.kamiblue.botkt.commands

import org.kamiblue.botkt.Command
import org.kamiblue.botkt.PermissionTypes.CREATE_RELEASE
import org.kamiblue.botkt.doesLaterIfHas
import org.kamiblue.botkt.helpers.ShellHelper.systemBash
import org.kamiblue.botkt.literal

// TODO hardcoded, pending plugin support
object ReleaseCommand : Command("release") {
    init {
        literal("major") {
            doesLaterIfHas(CREATE_RELEASE) {
                "./scripts/runAutomatedRelease.sh major".systemBash("/home/mika/kamiblue")
            }
        }

        literal("beta") {
            doesLaterIfHas(CREATE_RELEASE) {
                "./scripts/runAutomatedRelease.sh".systemBash("/home/mika/kamiblue")
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Usage:\n`$fullName major`\n`$fullName beta`"
    }
}
