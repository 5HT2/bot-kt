package org.kamiblue.botkt.command.commands.system

import net.ayataka.kordis.entity.message.MessageBuilder
import org.kamiblue.botkt.PermissionTypes.VIEW_LOGS
import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category
import org.kamiblue.botkt.command.MessageExecuteEvent
import org.kamiblue.botkt.utils.*
import org.kamiblue.botkt.utils.StringUtils.toHumanReadable
import org.kamiblue.commons.extension.max
import java.io.File
import java.nio.file.Files
import java.time.Instant

object UploadLogCommand : BotCommand(
    name = "uploadlog",
    alias = arrayOf("upl"),
    category = Category.SYSTEM
) {
    init {
        executeIfHas(VIEW_LOGS, "Upload the `debug.log`") {
            uploadLog("debug.log", LogType.DEBUG)
        }

        literal("list") {
            enum<LogType>("log type") { logTypeArg ->
                executeIfHas(VIEW_LOGS, "List available logs") {
                    File("logs/${logTypeArg.value.folder}").listFiles()?.filter { it.isFile }?.sortedBy { it.name }?.let {
                        channel.normal(it.joinToString("\n") { file -> "`${file.name}`" }.max(2048))
                    } ?: run {
                        channel.error("Could not find any logs inside `logs/${logTypeArg.value}`!")
                    }
                }
            }
        }

        enum<LogType>("log type") { logTypeArg ->
            string("log name") { logNameArg ->
                executeIfHas(VIEW_LOGS, "Upload a log of this type") {
                    uploadLog(logNameArg.value, logTypeArg.value)
                }
            }
        }

        greedy("log folder and name") { logNameArg ->
            executeIfHas(VIEW_LOGS, "Upload a log with this name and folder") {
                uploadLog(logNameArg.value, LogType.LATEST) // use latest because empty root folder
            }
        }
    }

    private suspend fun MessageExecuteEvent.uploadLog(logName: String, logType: LogType) {
        val file = File("logs/${logType.folder}$logName")
        if (!file.exists() || file.isDirectory) {
            channel.error("Could not find ${logType.getName()} log named `$logName`")
        } else {
            channel.upload(file, embed = getEmbed(logType, file))
        }
    }

    private fun getEmbed(logType: LogType, file: File) = MessageBuilder().apply {
        embed {
            field("Log Type", logType.name.toHumanReadable())
            field("Log Name", file.name)
            field("Last Modified", Instant.ofEpochMilli(file.lastModified()).prettyFormat())
            field("Size", "${Files.size(file.absoluteFile.toPath()) / 1000.0}KB")
            color = if (logType == LogType.ERROR) Colors.ERROR.color else Colors.PRIMARY.color
        }
    }.build()

    @Suppress("unused")
    private enum class LogType(val folder: String) {
        LATEST(""), DEBUG("debug/"), ERROR("error/");

        fun getName() = this.name.toLowerCase()
    }
}
