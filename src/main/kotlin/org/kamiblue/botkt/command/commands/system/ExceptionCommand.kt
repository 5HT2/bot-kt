package org.kamiblue.botkt.command.commands.system

import org.kamiblue.botkt.Main
import org.kamiblue.botkt.PermissionTypes
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.options.HasPermission
import org.kamiblue.botkt.utils.error
import org.kamiblue.botkt.utils.normal
import org.kamiblue.botkt.utils.success
import org.kamiblue.commons.extension.max

object ExceptionCommand : BotCommand(
    name = "exception",
    alias = arrayOf("exc"),
    category = Category.SYSTEM,
    description = "View recent exceptions caused in commands"
) {
    private val exceptions = ArrayDeque<Exception>(16)

    init {
        literal("list") {
            execute("List saved exceptions", HasPermission.get(PermissionTypes.COUNCIL_MEMBER)) {
                if (exceptions.isEmpty()) {
                    channel.success("No exceptions caught recently!")
                } else {
                    channel.normal(
                        exceptions.withIndex().joinToString(separator = "\n") { "`${it.index}`: `${it.value.message}`" }
                    )
                }
            }
        }

        int("index") { indexArg ->
            execute("Print a saved exception", HasPermission.get(PermissionTypes.COUNCIL_MEMBER)) {
                if (exceptions.isEmpty()) {
                    channel.success("No exceptions caught recently!")
                } else {
                    exceptions.getOrNull(indexArg.value)?.let {
                        message.channel.send("```\n" + it.stackTraceToString().max(1992) + "\n```")
                    } ?: run {
                        channel.error("Exception with index `${indexArg.value}` is not stored!")
                    }
                }
            }
        }
    }

    fun addException(e: Exception) {
        Main.logger.warn("Exception caught by ${javaClass.simpleName}", e)
        while (exceptions.size >= 10) {
            exceptions.removeFirstOrNull()
        }
        exceptions.add(e)
    }
}
