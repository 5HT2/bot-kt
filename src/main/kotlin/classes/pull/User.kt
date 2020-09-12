package classes.pull

data class User(
    var avatar_url: String = "",
    var events_url: String = "",
    var followers_url: String = "",
    var following_url: String = "",
    var gists_url: String = "",
    var gravatar_id: String = "",
    var html_url: String = "",
    var id: Long = 0,
    var login: String = "",
    var node_id: String = "",
    var organizations_url: String = "",
    var received_events_url: String = "",
    var repos_url: String = "",
    var site_admin: Boolean = false,
    var starred_url: String = "",
    var subscriptions_url: String = "",
    var type: String = "",
    var url: String = ""
)