import MathHelper.round
import ShellHelper.runCommandOutput
import java.io.File

object SystemHelper {
    /**
     * @return human-readable markdown-formatted CPU information
     */
    fun formattedCpus(): String {
        val cpus = getCpus()
        val speeds = arrayListOf<String>()

        for (i in 0 until cpus.size) {
            speeds.add("**[$i]** ${getCpuUsage(i).toString()}GHz")
        }

        val cpuInfo = "**${cpus.size}x** ${cpus[0]}\n"
        return cpuInfo + speeds.joinToString { it }.replace(", ", " ")
    }

    /**
     * @return CPU model information in an ArrayList
     */
    fun getCpus(): ArrayList<String> {
        val cpuInfo = "cat /proc/cpuinfo".runCommandOutput().split("\n")

        val filtered = arrayListOf<String>()

        cpuInfo.forEach {
            if (it.startsWith("model name")) {
                filtered.add(it.substring(13))
            }
        }

        return filtered
    }

    /**
     * @return CPU [core] usage in GHz
     */
    fun getCpuUsage(core: Int): Double? {
        val freqInKHz: Double
        try {
            freqInKHz = File("/sys/devices/system/cpu/cpu$core/cpufreq/scaling_cur_freq").readText().toDouble()
        } catch (ignored: NumberFormatException) {
            return null
        }

        return round(freqInKHz / 1000000, 2)
    }
}