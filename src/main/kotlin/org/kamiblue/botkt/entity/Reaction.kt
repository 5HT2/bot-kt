package org.kamiblue.botkt.entity

import com.google.gson.JsonObject
import net.ayataka.kordis.entity.server.emoji.PartialEmoji

data class Reaction(
    val emoji: PartialEmoji,
    val count: Int,
    val me: Boolean
) {

    constructor(jsonObject: JsonObject) :
        this(
            partialEmoji(jsonObject.getAsJsonObject("emoji")),
            jsonObject.get("count").asInt,
            jsonObject.get("me").asBoolean
        )
}
