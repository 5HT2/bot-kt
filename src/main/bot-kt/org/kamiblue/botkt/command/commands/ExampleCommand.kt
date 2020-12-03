package org.kamiblue.botkt.command.commands

import org.kamiblue.botkt.command.*

/**
 * @author l1ving
 * @since 2020/08/18 16:30
 */
object ExampleCommand : Command("ec") {
    init {
        literal("kami") {
            doesLater {
                message.channel.send("[$fullName] First argument!")
            }
            literal("blue") {
                doesLater {
                    message.channel.send("[$fullName] Second argument used after first argument!")
                }
            }
        }
        literal("foo") {
            doesLater { message.channel.send("[$fullName] Second argument used without first argument!") }
        }
        literal("count") {
            greedyString("sentence") {
                doesLater { context ->
                    // Explicit types are necessary for type inference
                    val sentence: String = context arg "sentence"
                    message.channel.send("[$fullName] There's ${sentence.length} characters in that sentence!")
                }
            }
        }
    }

    override fun getHelpUsage(): String {
        return "Examples for Brigadier's syntax.\n\n" +
                "Example for chaining arguments:\n" +
                "`$fullName kami [blue]`\n\n" +
                "Example for multiple first arguments:\n" +
                "`$fullName foo`\n\n" +
                "Example for greedy strings:\n" +
                "`$fullName count <some sentence you'd like to count>`"
    }
}
