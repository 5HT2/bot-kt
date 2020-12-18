package org.kamiblue.botkt.utils

import com.google.gson.JsonObject
import io.ktor.client.request.*
import net.ayataka.kordis.DiscordClientImpl
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.entity.user.UserImpl
import org.kamiblue.botkt.Main
import java.time.temporal.ChronoUnit

suspend fun requestUser(id : Long) : User {
    val jsonObject = Main.discordHttp.request<JsonObject> {
        url("https://discord.com/api/v8/users/$id")
    }
    return UserImpl(Main.client as DiscordClientImpl, jsonObject)
}

fun User.accountAge(unit: ChronoUnit = ChronoUnit.DAYS): Long {
    return timestamp.untilNow(unit)
}