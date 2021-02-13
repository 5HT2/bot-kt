package org.kamiblue.botkt

import org.kamiblue.botkt.manager.managers.ResponseManager
import org.kamiblue.botkt.utils.StringUtils.toRegexes

/**
 * [configPath] the file name on disk, OR a remote URL. If it is a URL, it must be a valid URL which includes http/https as a prefix
 * [data] the actual config data, read in the format of clazz
 */
enum class ConfigType(val configPath: String, var data: Any? = null) {
    PERMISSION("config/permissions.json"),
    TICKET("config/tickets.json"),
    ARCHIVE_CHANNEL("config/archived_channels.json"),
    RESPONSE("config/responses.json"),
    NAME_SANITIZING("config/name_sanitizing.json")
}

/**
 * [councilMembers] a hashmap of all the council members
 */
class PermissionConfig(val councilMembers: HashMap<Long, List<PermissionTypes>>)

/**
 * @param ticketCategory channel category of where to create tickets, required
 * @param ticketCreateChannel channel where people can create tickets, required
 * @param ticketPingRole role to ping when new ticket created, optional
 * @param ticketUploadChannel channel to upload tickets when closed, optional
 */
class TicketConfig(
    val ticketCategory: Long?,
    val ticketCreateChannel: Long?,
    val ticketPingRole: Long?,
    val ticketUploadChannel: Long?,
    var ticketTotalAmount: Int?
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
