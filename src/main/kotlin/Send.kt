import Main.Colors.SUCCESS
import Main.Colors.ERROR
import net.ayataka.kordis.entity.message.Message

object Send {
    suspend fun Message.success(description: String) {
        channel.send {
            embed {
                this.description = description
                color = SUCCESS.color
            }
        }
    }

    suspend fun Message.error(description: String) {
        channel.send {
            embed {
                this.title = "Error"
                this.description = description
                this.color = ERROR.color
            }
        }
    }
}