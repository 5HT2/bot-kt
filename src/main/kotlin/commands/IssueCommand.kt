package commands

import AuthConfig
import Command
import arg
import classes.IssueClass
import com.google.gson.Gson
import doesLater
import integer
import okhttp3.OkHttpClient
import okhttp3.Request
import string
import java.net.URL


@Suppress("BlockingMethodInNonBlockingContext")
object IssueCommand : Command("issue") {
    init {
        string("repo") {
            integer("issueNum") {
                doesLater { context ->
                    val gson = Gson()
                    val repo: String = context arg "repo"
                    val issueNum: Int = context arg "issueNum"
                    val httpClient = OkHttpClient()
                    val request = Request.Builder()
                        .url(URL("https://api.github.com/repos/kami-blue/${repo}/issues/${issueNum}"))
                        .addHeader("{headers: {Authorization: token ${FileManager.readConfig<AuthConfig>(ConfigType.AUTH, false)?.githubToken}}}", "application/json")
                        .build()
                    val response = httpClient.newCall(request).execute()
                    val responseBody = response.body!!.string()
                    val parsedJson = gson.fromJson(responseBody, IssueClass::class.java)
                    message.channel.send{
                        embed {
                            author("カミブルー！", "https://cdn.discordapp.com/avatars/638403216278683661/1e8bed04cb18e1cb1239e208a01893a1.png", "https://kamiblue.org")
                            thumbnailUrl(parsedJson.user.avatar_url)
                            field("URL To Issue:",parsedJson.html_url)
                            field("User:", parsedJson.user.login)
                            field("Body:", parsedJson.body)
                            field("Labels:", parsedJson.labels[0].name)
                            field("Milestone:", parsedJson.milestone.title)
                            field("Status", parsedJson.state)
                        }
                    }
                }
            }
        }

    }
}

private operator fun String?.invoke(avatarUrl: String) {

}

