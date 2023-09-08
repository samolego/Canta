import 'package:canta/app_info.dart';
import 'package:canta/applist.dart';
import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
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
    return ListView(
      children: filtered
          .map((app) => ListTile(
                leading: Image.memory(app.icon),
                title: Text(app.name),
                subtitle: Text(app.packageName),
                trailing: Observer(
                  builder: (context) => Checkbox(
                    value: appList.selectedApps.contains(app.packageName),
                    onChanged: (value) => _toggleApp(value, app.packageName),
                  ),
                ),
              ))
          .toList(),
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
