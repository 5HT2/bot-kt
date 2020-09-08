import com.google.gson.GsonBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author dominikaaaa
 * @since 2020/08/28 13:40
 */
object Pm2 {
    fun createJson(version: String) {
        val gson = GsonBuilder().setPrettyPrinting().create()

        if (File("process.json").exists()) {
            File("process.json").delete()
        }

        Files.newBufferedWriter(Paths.get("process.json")).use {
            it.write(gson.toJson(createProcess(version)))
            it.close()
        }
    }

    private fun createProcess(version: String): Pm2Process {
        val apps = Apps()
        val process = Pm2Process()
        apps.script = getJre()
        apps.args = listOf("-jar", "bot-kt-$version.jar")
        apps.watch = listOf("bot-kt-$version.jar")
        process.apps = listOf(apps)
        return process
    }

    private fun getJre(): String {
        var home = System.getProperty("java.home")
        println("Auto Update - Found Java Home \"$home\"")
        if (home.isEmpty() || !Regex("[^ ]").containsMatchIn(home)) {
            throw IllegalStateException(
                "Your Java home doesn't appear to be a valid path. " +
                        "This is required by pm2, please set your Java home properly."
            )
        }
        if (home.endsWith('/')) home = home.substring(0, home.length - 1)
        if (home.endsWith("jre")) home = home.substring(0, home.length - 3)
        if (home.endsWith('/')) home = home.substring(0, home.length - 1)

        home = "$home/bin/java"
        println("Auto Update - Saved Java binary as \"$home\"")
        return home
    }
}

/**
 * These are all automatically assigned, no need to worry about creating the config yourself.
 * [script] is the path to your JVM home, eg /home/mika/jdk1.8.0_261/bin/java
 * [args] is a listOf("-jar", "bot-kt-version.jar")
 * [watch] is a listOf("bot-kt-version.jar")
 */
data class Apps(
    val name: String = "bot-kt",
    val cwd: String = ".",
    var script: String? = null,
    var args: List<String>? = null,
    var watch: List<String>? = null,
    val node_args: List<String> = emptyList(),
    val log_date_format: String = "YYYY-MM-DD HH:mm Z",
    val exec_interpreter: String = "",
    val exec_mode: String = "fork"
)

/**
 * Just a holder class, due to the format of process.json for pm2.
 * If wanted, you could expand this to include multiple [Apps], just make sure to change [Apps.name]
 */
data class Pm2Process(
    var apps: List<Apps>? = null
)
