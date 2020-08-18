import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import commands.ExampleCommand
import java.util.*

fun main() {
    Scanner(System.`in`).use { s ->
        val dispatcher = CommandDispatcher<Cmd>()

        dispatcher.register(ExampleCommand)

        while (true) {
            print("> ")
            val line = s.nextLine()

            try {
                val exit = dispatcher.execute(line, Cmd())
                println("(executed with exit code $exit)")
            } catch (e: CommandSyntaxException) {
                println("You have a syntax error: ${e.message}")
            }
        }
    }
}
