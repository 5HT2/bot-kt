package org.kamiblue.botkt

import org.kamiblue.botkt.command.commands.github.IssueCommand
import org.kamiblue.botkt.manager.managers.ResponseManager
import org.kamiblue.botkt.utils.StringUtils.toRegexes

/**
 * [configPath] is the file name on disk, OR a remote URL. If it is a URL, it must be a valid URL which includes http/https as a prefix
 * [data] is the actual config data, read in the format of clazz
 */
enum class ConfigType(val configPath: String, var data: Any? = null) {
    AUTH("config/auth.json"),
    RULES("config/rules.json"),
    USER("config/user.json"),
    PERMISSION("config/permissions.json"),
    COUNTER("config/counters.json"),
    JOIN_LEAVE("config/joinleave.json"),
    TICKET("config/tickets.json"),
    STAR_BOARD("config/starboard.json"),
    LOGGING("config/logging.json"),
    ARCHIVE_CHANNEL("config/archived_channels.json"),
    RESPONSE("config/responses.json"),
    NAME_SANITIZING("config/name_sanitizing.json")
}

/**
 * [botToken] is the token given to you from https://discord.com/developers/applications/BOT_ID_HERE/bot
 * [githubToken] can be generated with the full "repo" access checked https://github.com/settings/tokens
 */
class AuthConfig(
    val botToken: String,
    val githubToken: String
)

/**
 * [rules] is a HashMap with the rule name/number as the key and the rule as the value
 */
class RulesConfig(val rules: HashMap<String, String>)

/**
 * [version] is a semver format version String
 * Checked by comparing [Main.currentVersion] against https://raw.githubusercontent.com/l1ving/bot-kt/master/version.json
 */
class VersionConfig(val version: String)

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
 * [unknownCommandError] is if you want a response when a command is ran with your [prefix], but there is no registered command for it
 * // TODO: refactor into module-specific settings
 */
class UserConfig(
    val autoUpdate: Boolean?,
    val autoUpdateRestart: Boolean?,
    val primaryServerId: Long?,
    val startUpChannel: String?,
    val statusMessage: String?,
    val statusMessageType: Int?,
    val defaultGithubUser: String?,
    val prefix: Char?,
    val defaultBanReason: String?,
    val unknownCommandError: Boolean?,
    val capeCommit: Boolean?
)

/**
 * [councilMembers] is a hashmap of all the council members
 */
class PermissionConfig(val councilMembers: HashMap<Long, List<PermissionTypes>>)

/**
 * [memberEnabled] is if the member counter is enabled.
 * [downloadEnabled] is if the download counter is enabled.
 * [memberChannel] is the voice channel ID for the desired member counter.
 * [downloadChannelTotal] is the voice channel ID for the desired total downloads counter.
 * [downloadChannelLatest] is the voice channel ID for the desired latest release downloads counter.
 * [downloadStableUrl] is the main / stable repository in the format of l1ving/bot-kt
 * [downloadNightlyUrl] is the alternate / nightly repository in the format of l1ving/bot-kt
 * [perPage] is the max releases per page when using the Github API. Defaults to 200
 */
class CounterConfig(
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
class JoinLeaveConfig(
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
 * @param ticketOpenedRole role to give to users who have opened tickets, to hide the new ticket channel, optional
 * @param ticketUploadChannel channel to upload tickets when closed, optional
 */
class TicketConfig(
    val ticketCategory: Long?,
    val ticketCreateChannel: Long?,
    val ticketPingRole: Long?,
    val ticketOpenedRole: Long?,
    val ticketUploadChannel: Long?,
    var ticketTotalAmount: Int?
)

/**
 * @param channels <Server ID, Channel ID> Star board channel for each server
 * @param messages <Message ID> Messages added to the star board
 * @param threshold Amount Star emoji reactions to be added to star board
 */
class StarBoardConfig(
    val channels: HashMap<Long, Long>,
    val messages: HashSet<Long>,
    val threshold: Int
)

/**
 * @param ignoreChannels Channel IDs to not log
 * @param ignorePrefix A message prefix to not log when editing a council member edits their message
 * @param loggingChannel Channel ID of where to log to
 */
class LoggingConfig(
    val ignoreChannels: HashSet<Long>?,
    val ignorePrefix: String?,
    val loggingChannel: Long?
)

class ArchivedChannelsConfig(
    var amount: Int?
)

/**
 * @param roleIgnorePrefix skip the ignored role list if the message starts with this prefix
 */
class ResponseConfig(
    val responses: List<ResponseManager.Response> = emptyList(),
    val roleIgnorePrefix: String? = ":",
    val ignoreChannels: HashSet<Long>?,
)

/**
 * @param removePrefix Remove non-allowed chars from the name
 * @param minNormalChars Minimum allows chars in a name before resetting
 * @param minNormalPercentage Minimum normal char percentage before resetting
 * @param wordListAmount Amount of words to choose from [wordList]
 * @param talkNameLimit How often to check someone's nickname after chatting, in milliseconds. Saves processing power.
 * @param allowedChars Allowed characters to be checked for [minNormalChars] and [minNormalPercentage]
 * @param wordList Words to choose from when resetting a name
 */
class NameSanitizingConfig(
    val removePrefix: Boolean,
    val minNormalChars: Int,
    val minNormalPercentage: Int,
    val wordListAmount: Int,
    val talkNameLimit: Int,
    val allowedChars: List<Char>,
    val wordList: List<String>,
    val ignoreRoles: Set<Long>?,
    private val disallowedRegexesList: List<String>
) {
    private var compiledRegexCache: List<Regex>? = null
    val disallowedRegexes
        get() = compiledRegexCache ?: synchronized(this) {
            disallowedRegexesList.toRegexes().also {
                Main.logger.debug("Creating regex cache \"$it\" for NameSanitizing")
                compiledRegexCache = it
            }
        }
}
