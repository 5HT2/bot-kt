package classes.search

import classes.issue.Milestone

data class Item(
    var active_lock_reason: Any? = null,
    var assignee: Any? = null,
    var assignees: List<Assignee>? = listOf(),
    var author_association: String = "",
    var body: String? = "",
    var closed_at: String? = null,
    var comments: Int = 0,
    var comments_url: String = "",
    var created_at: String = "",
    var draft: Boolean = false,
    var events_url: String = "",
    var html_url: String = "",
    var id: Long = 0,
    var labels: List<Label>? = listOf(),
    var labels_url: String? = "",
    var locked: Boolean = false,
    var milestone: Milestone? = Milestone(),
    var node_id: String = "",
    var number: Int = 0,
    var performed_via_github_app: Any? = null,
    var pull_request: PullRequest = PullRequest(),
    var repository_url: String = "",
    var score: Double = 0.0,
    var state: String = "",
    var title: String = "",
    var updated_at: String = "",
    var url: String = "",
    var user: User = User()
)