package classes.issue

data class Milestone(
    var closed_at: Any? = null,
    var closed_issues: Int = 0,
    var created_at: String = "",
    var creator: Creator = Creator(),
    var description: String = "",
    var due_on: Any? = null,
    var html_url: String = "",
    var id: Long = 0,
    var labels_url: String = "",
    var node_id: String = "",
    var number: Int = 0,
    var open_issues: Int = 0,
    var state: String = "",
    var title: String = "",
    var updated_at: String = "",
    var url: String = ""
)