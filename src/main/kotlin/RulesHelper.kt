import net.ayataka.kordis.entity.message.Message

object RulesHelper {
    suspend fun getRulesMessage(message: Message): String? {
        return (message.server!!.textChannels.findByName("rules") ?: return null).getMessages().first().content
    }

    suspend fun getRules(message: Message): HashMap<String, String> {
        val rulesMap: HashMap<String, String> = HashMap()
        val noFormatRules = getRulesMessage(message) ?: return rulesMap
        val rulesByLine = noFormatRules.split("\n")
        for (ruleLine in rulesByLine) {
            val ruleSplit = ruleLine.split(" ", limit = 2)
            rulesMap[ruleSplit[0]] = ruleSplit[1]
        }
        return rulesMap
    }

    suspend fun getRule(message: Message, rule: String): String? {
        return getRules(message)[rule]
    }
}