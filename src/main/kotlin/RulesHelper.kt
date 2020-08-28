import net.ayataka.kordis.entity.message.Message
import net.ayataka.kordis.entity.server.Server

object RulesHelper {
    suspend fun getRulesMessage(server: Server): String? {
        return (server.textChannels.findByName("rules") ?: return null).getMessages().first().content
    }

    suspend fun getRules(server: Server): HashMap<String, String> {
        val rulesMap: HashMap<String, String> = HashMap()
        val noFormatRules = getRulesMessage(server) ?: return rulesMap
        val rulesByLine = noFormatRules.split("\n")
        for (ruleLine in rulesByLine) {
            val ruleSplit = ruleLine.split(" ", limit = 2)
            rulesMap[ruleSplit[0]] = ruleSplit[1]
        }
        return rulesMap
    }

    suspend fun getRule(server: Server, rule: String): String? {
        return getRules(server)[rule]
    }
}