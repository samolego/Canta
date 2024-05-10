package org.samo_lego.canta.util

import java.util.Locale

class Filter(val name: String, val shouldShow: (AppInfo) -> Boolean) {
    companion object {
        val any: Filter
        val availableFilters: List<Filter>

        init {
            any = Filter(name = "Any", shouldShow = { true })
            availableFilters = RemovalRecommendation.entries.map { entry ->
                Filter(
                    name = entry.toString().lowercase(Locale.ROOT)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    shouldShow = { app -> app.removalInfo == entry }
                )
            } + any
        }
    }
}
