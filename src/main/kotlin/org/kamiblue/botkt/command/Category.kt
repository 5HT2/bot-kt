package org.kamiblue.botkt.command

import org.kamiblue.command.Command
import org.kamiblue.commons.interfaces.DisplayEnum

enum class Category(
    override val displayName: String,
    val commands: LinkedHashSet<Command<MessageExecuteEvent>> = LinkedHashSet()
) : DisplayEnum {
    FUN("Fun"),
    GITHUB("Github"),
    INFO("Info"),
    MISC("Misc"),
    MODERATION("Moderation"),
    SYSTEM("System");

    override fun toString(): String {
        return displayName
    }
}