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
            it.source later {
                message.channel.send("[$name] First argument!")
            }
            0
        }.then(literal<Cmd>("2").executes {
            it.source later {
                message.channel.send("[$name] Second argument used after first argument!")
            }
            0
        })).then(literal<Cmd>("3").executes {
            it.source later {
                message.channel.send("[$name] Second argument used without first argument!")
            }
            0
        })
    }
}
