package org.kamiblue.botkt.command.commands.github

import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.helpers.ShellHelper.systemBash

// TODO hardcoded, pending plugin support
object ReleaseCommand : BotCommand(
    name = "release",
    category = Category.GITHUB,
) {
    init {
        literal("major") {
            executeIfHas(PermissionTypes.CREATE_RELEASE) {
                "./scripts/runAutomatedRelease.sh major".systemBash("/home/mika/kamiblue")
            }
        }

        literal("beta") {
            executeIfHas(PermissionTypes.CREATE_RELEASE) {
                "./scripts/runAutomatedRelease.sh".systemBash("/home/mika/kamiblue")
            }
        }
    }
}
