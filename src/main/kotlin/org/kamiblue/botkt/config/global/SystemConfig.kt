package org.kamiblue.botkt.config.global

import net.ayataka.kordis.entity.server.enums.ActivityType
import org.kamiblue.botkt.config.GlobalConfig

object SystemConfig : GlobalConfig("System") {
    val botToken by setting("Bot Token", "", "Discord bot token from https://discord.com/developers/applications/BOT_ID_HERE/bot", true)

    val autoUpdate by setting("Auto Update", true, "Automatically update the bot after a successful update check.")
    val autoUpdateRestart by setting("Auto Update Restart", true, "Restart after auto update.")

    val startupServer by setting("Startup Server", -1L, "ID of the server to send startup message. -1 to send to all servers.")
    val startUpChannel by setting("Startup Channel", "", "Name of the channel to send startup message, empty to disable.")

    val statusMessage by setting("Status Message", "Insert Message Here", "Bot status message on Discord.")
    val statusType by setting("Status Type", ActivityType.PLAYING, "Type of status.")

    val prefix by setting("Prefix", ';', "Command prefix for the bot.")
    val unknownCommandError by setting("Unknown Command Error", false, "Sends an error message for unknown command")
}
