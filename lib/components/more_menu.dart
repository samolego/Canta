import 'package:canta/util/filters.dart';
import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:mobx/mobx.dart';

class MoreMenu extends StatelessWidget {
  final VoidCallback clearSelectedApps;
  final Function(bool? value, Filter filter) toggleFilter;
  final ObservableSet<Filter> filters;
  final Function(Filter? removalFilter) removalTypeFilter;
  final Filter? selectedRemovalTypeFilter;

  const MoreMenu({
    super.key,
    required this.clearSelectedApps,
    required this.toggleFilter,
    required this.filters,
    required this.removalTypeFilter,
    this.selectedRemovalTypeFilter,
  });

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<void>(
      child: const Icon(Icons.more_vert),
      itemBuilder: (BuildContext context) {
        final List<PopupMenuItem<void>> items = Filter.availableFilters
            .map((fltr) => PopupMenuItem<void>(
                onTap: () => toggleFilter(!filters.contains(fltr), fltr),
                child: Observer(
                  builder: (_) => Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(fltr.name),
                      Checkbox(
                        value: filters.contains(fltr),
                        onChanged: (value) => toggleFilter(value, fltr),
                      ),
                    ],
                  ),
                )))
            .toList();
        items.add(
          PopupMenuItem<void>(
            child: DropdownMenu<Filter?>(
              inputDecorationTheme: const InputDecorationTheme(
                // No border
                border: InputBorder.none,
              ),
              textStyle: const TextStyle(
                fontSize: 16,
              ),
              onSelected: (value) => removalTypeFilter(value),
              label: const Text("Removal type"),
              initialSelection: selectedRemovalTypeFilter,
              dropdownMenuEntries: [
                const DropdownMenuEntry<Filter?>(
                  label: "Any",
                  value: null,
                ),
                ...Filter.availableRemovals
                    .map(
                      (e) => DropdownMenuEntry<Filter?>(
                        label: e.name,
                        value: e,
                      ),
                    )
                    .toList(),
              ],
            ),
          ),
        );

        items.add(
          PopupMenuItem<void>(
            onTap: clearSelectedApps,
            child: const Text("Deselect all apps"),
          ),
        );
        return items;
      },
    );
  }
}
