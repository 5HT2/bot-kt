package utils

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

@Suppress("BlockingMethodInNonBlockingContext")
inline fun <reified T> request(token: String, url: String): T {
    val request = Request.Builder().addHeader("Authorization", "token $token").url(url).get().build()
    val response = OkHttpClient().newCall(request).execute()

    return Gson().fromJson(response.body()!!.string(), T::class.java)
}