import Main.currentVersion
import java.io.File

object UpdateHelper {
    fun updateCheck() {
        if (File("noUpdateCheck").exists()) return
        val versionConfig = FileManager.readConfig<VersionConfig>(ConfigType.VERSION, false)

        if (versionConfig?.version == null) {
            println("Couldn't access remote version when checking for update")
            return
        }

        if (versionConfig.version != currentVersion) {
            println("Not up to date:\nCurrent version: $currentVersion\nLatest Version: ${versionConfig.version}\n")
        } else {
            println("Up to date! Running on $currentVersion")
        }
    }

    /**
     * TODO: FINISH
     */
    fun updateBot(version: String) {
        if (!File("autoUpdate").exists()) return

        println()
    }
}