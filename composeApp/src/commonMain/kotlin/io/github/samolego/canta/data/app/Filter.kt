package io.github.samolego.canta.data.app

/**
 * Filter for the app list.
 * @param name Name of the filter.
 * @param shouldShow Function to determine if the app should be shown.
 */
class Filter(
    val name: String,
    val shouldShow: (AppInfo) -> Boolean,
    val badgeInfo: AppBadgeInfo? = null
) {
    companion object {
        private fun capitalize(value: String): String =
            value.lowercase().replaceFirstChar { it.uppercase() }

        /**
         * Filter to show all apps.
         */
        val any: Filter = Filter(name = "Any", shouldShow = { true })

        val user = Filter(name = "User", shouldShow = { app -> !app.isSystemApp })

        /**
         * List of available filters.
         */
        val availableFilters: List<Filter>

        init {
            val removalFilters =
                AppBadgeInfo.entries.filter { AppBadgeInfo.SYSTEM != it }
                    .map { entry ->
                        Filter(
                            name = capitalize(entry.toString()),
                            shouldShow = { app -> app.badgeInfo == entry },
                            badgeInfo = entry
                        )
                    }.toMutableList()
            removalFilters.add(0, any)

            removalFilters.add(1, user)

            val unclassified =
                Filter(name = "Unclassified", shouldShow = { app -> app.badgeInfo == null })
            removalFilters.add(2, unclassified)

            val disabled = Filter(name = "Disabled", shouldShow = { app -> app.isDisabled })
            removalFilters.add(3, disabled)

            availableFilters = removalFilters
        }
    }
}
