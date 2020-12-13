package org.kamiblue.botkt

import org.kamiblue.botkt.command.commands.github.IssueCommand

/**
 * [configPath] is the file name on disk, OR a remote URL. If it is a URL, it must be a valid URL which includes http/https as a prefix
 * [data] is the actual config data, read in the format of clazz
 */
enum class ConfigType(val configPath: String, var data: Any? = null) {
    AUTH("config/auth.json"),
    MUTE("config/mutes.json"),
    RULES("config/rules.json"),
    USER("config/user.json"),
    PERMISSION("config/permissions.json"),
    COUNTER("config/counters.json"),
    JOIN_LEAVE("config/joinleave.json"),
    TICKET("config/tickets.json")
}

/**
 * [botToken] is the token given to you from https://discord.com/developers/applications/BOT_ID_HERE/bot
 * [githubToken] can be generated with the full "repo" access checked https://github.com/settings/tokens
 */
data class AuthConfig(
    val botToken: String,
    val githubToken: String
)

/**
 * [id] is the user snowflake ID
 * [unixUnmute] is the UNIX time of when they should be unmuted.
 * When adding a new [unixUnmute] time, it should be current UNIX time + mute time in seconds
 */
data class MuteConfig(
    val id: Long,
    val unixUnmute: Long
)

/**
 * [rules] is a HashMap with the rule name/number as the key and the rule as the value
 */
data class RulesConfig(val rules: HashMap<String, String>)

/**
 * [version] is a semver format version String
 * Checked by comparing [Main.currentVersion] against https://raw.githubusercontent.com/kami-blue/bot-kt/master/version.json
 */
data class VersionConfig(val version: String)

/**
 * [autoUpdate] is whether the bot should automatically update after a successful update check. Will not do anything when set to true if update checking is disabled.
 * [autoUpdateRestart] is whether the bot should restart after automatically updating.
 * [primaryServerId] is the main server where startup messages should be sent. Omit from config to send to all servers.
 * [startUpChannel] is the channel name of where to send bot startup messages. Omit from config to disable startup messages.
 * [statusMessage] is the bot status message on Discord.
 * [statusMessageType] is the type of status. Playing is 0, Streaming is 1, Listening is 2 and Watching is 3.
 * [defaultGithubUser] is the default user / org used in the [IssueCommand].
 * [prefix] is the single character command prefix. Defaults to ;
 * [defaultBanReason] is the default Reason for ban.
 * [issueCreationChannel] is the channel where issue creation is allowed. Leave null to allow in any channel
 * [unknownCommandError] is if you want a response when a command is ran with your [prefix], but there is no registered command for it
 * // TODO: refactor into module-specific settings
 */
data class UserConfig(
    val autoUpdate: Boolean?,
    val autoUpdateRestart: Boolean?,
    val primaryServerId: Long?,
    val startUpChannel: String?,
    val statusMessage: String?,
    val statusMessageType: Int?,
    val defaultGithubUser: String?,
    val prefix: Char?,
    val defaultBanReason: String?,
    val issueCreationChannel: Long?,
    val unknownCommandError: Boolean?,
    val capeCommit: Boolean?
)

/**
 * [councilMembers] is a hashmap of all the council members
 */
data class PermissionConfig(val councilMembers: HashMap<Long, List<PermissionTypes>>)

/**
 * [memberEnabled] is if the member counter is enabled.
 * [downloadEnabled] is if the download counter is enabled.
 * [memberChannel] is the voice channel ID for the desired member counter.
 * [downloadChannelTotal] is the voice channel ID for the desired total downloads counter.
 * [downloadChannelLatest] is the voice channel ID for the desired latest release downloads counter.
 * [downloadStableUrl] is the main / stable repository in the format of kami-blue/bot-kt
 * [downloadNightlyUrl] is the alternate / nightly repository in the format of kami-blue/bot-kt
 * [perPage] is the max releases per page when using the Github API. Defaults to 200
 */
data class CounterConfig(
    val memberEnabled: Boolean?,
    val downloadEnabled: Boolean?,
    val memberChannel: Long?,
    val downloadChannelTotal: Long?,
    val downloadChannelLatest: Long?,
    val downloadStableUrl: String?,
    val downloadNightlyUrl: String?,
    val perPage: Int?
)

/**
 * @param embed format messages as embeds or not
 * @param kickTooNew kick accounts less than 24 hours old
 * @param banRepeatedJoin ban accounts which were kicked 3 times
 */
data class JoinLeaveConfig(
    val joinChannel: Long?,
    val leaveChannel: Long?,
    val banChannel: Long?,
    val embed: Boolean?,
    val kickTooNew: Boolean?,
    val banRepeatedJoin: Boolean?
)

/**
 * @param ticketCategory channel category of where to create tickets, required
 * @param ticketCreateChannel channel where people can create tickets, required
 * @param ticketPingRole role to ping when new ticket created, optional
 */
data class TicketConfig(
    val ticketCategory: Long?,
    val ticketCreateChannel: Long?,
    val ticketPingRole: Long?
)