package org.samo_lego.canta.util

import java.util.Locale

/**
 * Filter for the app list.
 * @param name Name of the filter.
 * @param shouldShow Function to determine if the app should be shown.
 */
class Filter(
    val name: String,
    val shouldShow: (AppInfo) -> Boolean,
    val removalRecommendation: RemovalRecommendation? = null
) {
    companion object {
        /**
         * Filter to show all apps.
         */
        val any: Filter = Filter(name = "Any", shouldShow = { true })

        /**
         * List of available filters.
         */
        val availableFilters: List<Filter>

        init {
            // Filters are generated from the RemovalRecommendation enum.
            val removalFilters =
                RemovalRecommendation.entries.filter { RemovalRecommendation.SYSTEM != it }
                    .map { entry ->
                        Filter(
                            name = entry.toString().lowercase(Locale.ROOT)
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            shouldShow = { app -> app.removalInfo == entry },
                            removalRecommendation = entry
                        )
                    }.toMutableList()
            removalFilters.add(0, any)

            // Apps that are not system apps.
            val user = Filter(name = "User", shouldShow = { app -> !app.isSystemApp })
            removalFilters.add(1, user)

            val unclassified =
                Filter(name = "Unclassified", shouldShow = { app -> app.removalInfo == null })
            removalFilters.add(2, unclassified)

            // Apps that are disabled
            val disabled = Filter(name = "Disabled", shouldShow = { app -> app.isDisabled })
            removalFilters.add(3, disabled)

            availableFilters = removalFilters
        }
    }
}
