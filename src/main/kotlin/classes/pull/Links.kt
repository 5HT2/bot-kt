package classes.pull

data class Links(
    var comments: Comments = Comments(),
    var commits: Commits = Commits(),
    var html: Html = Html(),
    var issue: Issue = Issue(),
    var review_comment: ReviewComment = ReviewComment(),
    var review_comments: ReviewComments = ReviewComments(),
    var self: Self = Self(),
    var statuses: Statuses = Statuses()
)