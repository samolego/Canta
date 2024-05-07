package org.samo_lego.canta.util

data class Filter(private val name: String, val shouldShow: (AppInfo) -> Boolean)


/*static final List<Filter> _filters = [];
static final List<Filter> _removals = [];

static List<Filter> get availableFilters
{
    if (_filters.isEmpty) {
        _filters.addAll(
            [
                Filter("Only system", (app) => app . isSystemApp
        ),
        ]);
    }

    return _filters;
}

static List<Filter> get availableRemovals
{
    if (_removals.isEmpty) {
        for (final value in RemovalInfo.values) {
            final filterName =
            value.toString().split(".")[1].replaceAll("_", " ").toLowerCase();

            // Capitalize first letter
            final name = filterName [0].toUpperCase() + filterName.substring(1);

            _removals.add(Filter(name, (app) => app . removalInfo == value));
        }
    }

    return _removals;
}*/