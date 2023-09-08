import 'package:canta/applist.dart';
import 'package:canta/filters.dart';
import 'package:canta/search.dart';
import 'package:canta/tiles.dart';
import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:mobx/mobx.dart';

import 'app_info.dart';

void main() {
  runApp(const CantaApp());
}

class CantaApp extends StatelessWidget {
  static const String title = 'Canta';

  const CantaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: title,
      theme: ThemeData(
        primarySwatch: Colors.orange,
      ),
      home: const HomePage(title: title),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final AppList appList = AppList();

  @observable
  ObservableList<Function(AppInfo)> filters = ObservableList();

  @observable
  final ObservableList<Row> installedAppRows = ObservableList<Row>();

  @override
  void initState() {
    super.initState();
    _buildInstalledAppList();
    _buildUninstalledAppList();
  }

  Observer _getInstalledAppList() {
    return Observer(
      builder: (_) => installedAppRows.isEmpty
          ? Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: const [
                Text("Loading apps ..."),
                SizedBox(height: 16.0),
                CircularProgressIndicator(),
              ],
            )
          : ListView.builder(
              itemCount: installedAppRows.length,
              itemBuilder: (_, index) => installedAppRows[index],
            ),
    );
  }

  @action
  Future<void> _buildInstalledAppList() async {
    installedAppRows.clear();
    final Set<AppInfo> allApps = await appList.getInstalledApps();

    Set<AppInfo> filteredApps = allApps.where((appInfo) {
      return filters.every((filter) => filter(appInfo));
    }).toSet();

    for (var app in filteredApps) {
      installedAppRows.add(
        Row(
          key: Key(app.packageName),
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Flexible(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Image.memory(
                          app.icon,
                          width: 48,
                          height: 48,
                        ),
                      ),
                      Flexible(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Padding(
                              padding:
                                  const EdgeInsets.symmetric(vertical: 8.0),
                              child: Text(app.name,
                                  style: const TextStyle(fontSize: 22)),
                            ),
                            Text(app.packageName),
                            if (app.isSystemApp) getBadgeElement(),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            Observer(
              builder: (_) => Checkbox(
                value: appList.selectedApps.contains(app.packageName),
                onChanged: (value) => _toggleApp(value, app.packageName),
              ),
            ),
          ],
        ),
      );
    }
  }

  @observable
  final ObservableList<Row> uninstalledAppRows =
      ObservableList<InstallableAppTile>();

  @action
  Future<void> _buildUninstalledAppList() async {
    uninstalledAppRows.clear();
    final Set<String> allApps = await appList.getUninstalledApps();

    for (var packageName in allApps) {
      uninstalledAppRows.add(InstallableAppTile(
        packageName: packageName,
        isSelected: () => appList.selectedApps.contains(packageName),
        onCheck: _toggleApp,
      ));
    }
  }

  Scaffold _getUninstalledAppList() {
    return Scaffold(
      body: Observer(
        builder: (_) => uninstalledAppRows.isEmpty
            ? const Scaffold(
                body: Center(
                  child: Padding(
                    padding: EdgeInsets.all(16.0),
                    child: Text("No recoverable uninstalled apps found.",
                        style: TextStyle(fontSize: 18)),
                  ),
                ),
              )
            : ListView.builder(
                itemCount: uninstalledAppRows.length,
                itemBuilder: (_, index) => uninstalledAppRows[index]),
      ),
      floatingActionButton: Observer(
          builder: (_) => appList.selectedApps.isEmpty
              ? const SizedBox()
              : FloatingActionButton(
                  backgroundColor: Colors.green,
                  onPressed: _reinstallApps,
                  tooltip: 'Reinstall apps.',
                  child: const Icon(
                    Icons.install_mobile,
                    color: Colors.white,
                  ),
                )),
    );
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          bottom: const TabBar(
            tabs: [
              Tab(icon: Icon(Icons.auto_delete_outlined)),
              Tab(icon: Icon(Icons.delete_forever)),
            ],
          ),
          title: Text(widget.title, style: const TextStyle(fontSize: 24)),
          actions: [
            IconButton(
              onPressed: () {
                showSearch(
                  context: context,
                  delegate: AppSearch(appList: appList),
                );
              },
              icon: const Icon(Icons.search),
            ),
            IconButton(
              onPressed: () {},
              icon: PopupMenuButton<void>(
                child: const Icon(Icons.more_vert),
                itemBuilder: (BuildContext context) {
                  final List<PopupMenuItem<void>> items = Filter.available
                      .map((fltr) => PopupMenuItem<void>(
                              child: Observer(
                            builder: (_) => Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(fltr.name),
                                Checkbox(
                                  value: filters.contains(fltr.filterFn),
                                  onChanged: (value) =>
                                      _toggleFilter(value, fltr),
                                ),
                              ],
                            ),
                          )))
                      .toList();
                  items.add(PopupMenuItem<String>(
                    child: const Text("Deselect all"),
                    onTap: () => clearSelectedApps(),
                  ));
                  return items;
                },
              ),
            ),
          ],
        ),
        body: TabBarView(
          children: [
            Scaffold(
              body: Center(
                child: _getInstalledAppList(),
              ),
              floatingActionButton: FloatingActionButton(
                backgroundColor: Colors.red,
                onPressed: _uninstallApps,
                tooltip: 'Uninstall',
                child: const Icon(
                  Icons.delete,
                  color: Colors.white,
                ),
              ),
            ),
            Center(
              child: _getUninstalledAppList(),
            ),
          ],
        ),
      ),
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

  @action
  void _toggleFilter(bool? value, Filter filter) async {
    if (value!) {
      filters.add(filter.filterFn);
    } else {
      filters.remove(filter.filterFn);
    }
  }

  @action
  void _uninstallApps() {
    // Create a copy of selectedApps to avoid modifying it while iterating
    final List<String> appsToRemove = List.from(appList.selectedApps);

    for (var packageName in appsToRemove) {
      installedAppRows
          .removeWhere((element) => element.key == Key(packageName));
      uninstalledAppRows.add(InstallableAppTile(
        packageName: packageName,
        isSelected: () => appList.selectedApps.contains(packageName),
        onCheck: _toggleApp,
      ));

      appList.uninstallApp(packageName);
    }
    appList.selectedApps.clear();
  }

  @action
  Future<void> _reinstallApps() async {
    for (var packageName in appList.selectedApps) {
      uninstalledAppRows
          .removeWhere((element) => element.key == Key(packageName));
      await appList.reinstallApp(packageName);

      // Todo: add to installed section
    }
    appList.selectedApps.clear();
  }

  Badge getBadgeElement() {
    return Badge(
      label: Row(
        children: const [
          Icon(Icons.android),
          SizedBox(width: 8.0),
          Text("System App"),
        ],
      ),
      backgroundColor: Colors.redAccent,
    );
  }

  @action
  void clearSelectedApps() {
    appList.selectedApps.clear();
  }
}
