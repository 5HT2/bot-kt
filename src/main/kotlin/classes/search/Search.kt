package classes.search

data class Search(
    var incomplete_results: Boolean = false,
    var items: List<Item>? = listOf(),
    var total_count: Int = 0
)