package classes.pull

data class Base(
    var label: String = "",
    var ref: String = "",
    var repo: Repo = Repo(),
    var sha: String = "",
    var user: User = User()
)