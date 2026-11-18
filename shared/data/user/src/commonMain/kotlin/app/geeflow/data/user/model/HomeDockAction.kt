package app.geeflow.data.user.model

/** Stable preference identifiers: an unavailable machine action keeps its saved place. */
enum class HomeDockAction(val key: String) {
    Profiles("profiles"), Charts("charts"), History("history"), FreeControl("free-control"),
    Flush("flush"), HotWater("hot-water"), Scale("scale"), Brew("brew");

    companion object {
        val defaultOrder: List<HomeDockAction> = entries.toList()
        fun normalize(order: List<HomeDockAction>): List<HomeDockAction> = order.distinct() + defaultOrder.filterNot(order::contains)
        fun decode(value: String?): List<HomeDockAction> = normalize(value.orEmpty().split(',')
            .mapNotNull { key -> entries.firstOrNull { it.key == key } })
        fun encode(order: List<HomeDockAction>): String = normalize(order).joinToString(",") { it.key }

        /** Replace visible slots only, retaining e.g. a hidden hot-water item's position. */
        fun mergeVisible(saved: List<HomeDockAction>, visible: List<HomeDockAction>): List<HomeDockAction> {
            val remaining = visible.distinct().iterator()
            val visibleSet = visible.toSet()
            return normalize(saved).map { if (it in visibleSet) remaining.next() else it }
        }
    }
}
