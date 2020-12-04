package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.PermissionTypes.CREATE_RELEASE
import org.kamiblue.botkt.command.CommandOld
import org.kamiblue.botkt.command.doesLaterIfHas
import org.kamiblue.botkt.command.literal
import org.kamiblue.botkt.helpers.ShellHelper.systemBash

// TODO hardcoded, pending plugin support
object ReleaseCommand : CommandOld("release") {
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
