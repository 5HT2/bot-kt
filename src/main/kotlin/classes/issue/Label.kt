package classes.issue

data class Label(
    var color: String = "",
    var default: Boolean = false,
    var description: String? = "",
    var id: Long = 0,
    var name: String = "",
    var node_id: String = "",
    var url: String = ""
)