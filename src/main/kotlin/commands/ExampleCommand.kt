package commands

import Command
import arg
import doesLater
import greedyString
import literal

/**
 * @author dominikaaaa
 * @since 2020/08/18 16:30
 */
object ExampleCommand : Command("ec") {
    init {
        literal("kami") {
            doesLater {
                message.channel.send("[$name] First argument!")
            }
            literal("blue") {
                doesLater {
                    message.channel.send("[$name] Second argument used after first argument!")
                }
            }
        }
        literal("foo") {
            doesLater { message.channel.send("[$name] Second argument used without first argument!") }
        }
        literal("count") {
            greedyString("sentence") {
                doesLater { context ->
                    // Explicit types are necessary for type inference
                    val sentence: String = context arg "sentence"
                    message.channel.send("[$name] There's ${sentence.length} characters in that sentence!")
                }
            }
        }
    }
}
