package commands

import Cmd
import Command

/**
 * @author dominikaaaa
 * @since 2020/08/18 16:30
 */
object ExampleCommand : Command("ec") {
    init {
        then(literal<Cmd>("1").executes {
            println("[$name] First argument!")
            0
        }.then(literal<Cmd>("2").executes {
            println("[$name] Second argument used after first argument!")
            0
        })).then(literal<Cmd>("3").executes {
            println("[$name] Second argument used without first argument!")
            0
        })
    }
}