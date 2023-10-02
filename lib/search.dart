import 'package:canta/components/tiles.dart';
import 'package:canta/util/app_info.dart';
import 'package:canta/util/applist.dart';
import 'package:flutter/material.dart';
import 'package:mobx/mobx.dart';

class AppSearch extends SearchDelegate<String> {
  final AppList appList;

  AppSearch({required this.appList}) : super(searchFieldLabel: "Search apps");

  @override
  List<Widget> buildActions(BuildContext context) {
    // Actions for the AppBar (e.g., clear query button)
    return [
      IconButton(
        icon: const Icon(Icons.clear),
        onPressed: () {
          query = "";
        },
      ),
    ];
  }

  @override
  Widget buildLeading(BuildContext context) {
    // Leading icon on the left side of the AppBar
    return IconButton(
      icon: const Icon(Icons.arrow_back),
      onPressed: () {
        close(context, "");
      },
    );
  }

  @override
  Widget buildResults(BuildContext context) {
    return buildSuggestions(context);
  }

  @override
  Widget buildSuggestions(BuildContext context) {
    final Iterable<AppInfo> filtered = appList.installedApps.where((app) =>
        app.name.toLowerCase().contains(query.toLowerCase()) ||
        app.packageName.toLowerCase().contains(query.toLowerCase()));

    return _filteredApps(filtered);
  }

  Widget _filteredApps(Iterable<AppInfo> filtered) {
    final apps = filtered
        .map((app) => InstalledAppTile(
        appInfo: app,
            onCheck: (value) => _toggleApp(value, app.packageName),
            isSelected: () => appList.selectedApps.contains(app.packageName)))
        .toList();
    return ListView.builder(
      itemCount: apps.length,
      itemBuilder: (BuildContext context, int index) => apps[index],
    );
  }

  @action
  void _toggleApp(bool? value, String packageName) {
    if (value!) {
      appList.selectedApps.add(packageName);
    } else {
      appList.selectedApps.remove(packageName);
    }
  }
}
