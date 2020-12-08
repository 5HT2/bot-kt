package org.kamiblue.botkt.command.commands.misc

import org.kamiblue.botkt.command.BotCommand
import org.kamiblue.botkt.command.Category

object ExampleCommand : BotCommand(
    name = "example",
    alias = arrayOf("ex"),
    category = Category.MISC,
    description = "Example Command for kami-blue.command"
) {
    init {
        literal("kami") {
            execute("Example for chaining arguments") {
                message.channel.send("[$name] First argument!")
            }
            literal("blue") {
                execute("Example for chaining arguments") {
                    message.channel.send("[$name] Second argument used after first argument!")
                }
            }
        }

        literal("foo") {
            execute("Example for multiple literal arguments") {
                message.channel.send("[$name] Second argument used without first argument!")
            }
        }

        literal("count") {
            greedy("sentence") { sentenceArg ->
                execute("Example for greedy strings") {
                    val sentence = sentenceArg.value
                    message.channel.send("[$name] There's ${sentence.length} characters in that sentence!")
                }
            }
        }
    }
}
