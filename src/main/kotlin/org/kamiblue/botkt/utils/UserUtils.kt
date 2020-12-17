package org.kamiblue.botkt.utils

import com.google.gson.JsonParser
import net.ayataka.kordis.DiscordClientImpl
import net.ayataka.kordis.entity.user.User
import net.ayataka.kordis.entity.user.UserImpl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kamiblue.botkt.Main
import java.time.temporal.ChronoUnit

fun requestUser(id : Long) : User {
    val request = Request.Builder().addHeader("Authorization", "Bot ${getAuthToken()}").url("https://discord.com/api/v8/users/$id").get().build()
    val response = OkHttpClient().newCall(request).execute()
    val jsonObject = JsonParser.parseString(response.body?.string()).asJsonObject
    return UserImpl(Main.client as DiscordClientImpl, jsonObject)
}

fun User.accountAge(unit: ChronoUnit = ChronoUnit.DAYS): Long {
    return timestamp.untilNow(unit)
}