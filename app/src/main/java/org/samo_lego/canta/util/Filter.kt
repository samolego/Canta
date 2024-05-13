package org.samo_lego.canta.util

import java.util.Locale

/**
 * Filter for the app list.
 * @param name Name of the filter.
 * @param shouldShow Function to determine if the app should be shown.
 */
class Filter(val name: String, val shouldShow: (AppInfo) -> Boolean) {
    companion object {
        /**
         * Filter to show all apps.
         */
        val any: Filter

        /**
         * List of available filters.
         */
        val availableFilters: List<Filter>

        init {
            any = Filter(name = "Any", shouldShow = { true })

            // Filters are generated from the RemovalRecommendation enum.
            val removalFilters = RemovalRecommendation.entries.map { entry ->
                Filter(
                    name = entry.toString().lowercase(Locale.ROOT)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    shouldShow = { app -> app.removalInfo == entry }
                )
            }.toMutableList()
            removalFilters.add(0, any)

            availableFilters = removalFilters
        }
    }
}
