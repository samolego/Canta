import 'package:canta/applist.dart';
import 'package:canta/dialogue.dart';
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
  final ObservableList<InstalledAppTile> installedAppRows = ObservableList();

  @override
  void initState() {
    super.initState();
    appList.kotlinBind.checkShizuku().then((value) {
      if (!value) {
        showDialog(context: context, builder: (_) => const ShizukuDialog());
      }
    });

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
      _addInstalledApp(app);
    }
  }

  @action
  void _addInstalledApp(AppInfo appInfo) {
    installedAppRows.add(
      InstalledAppTile(
        appInfo: appInfo,
        onCheck: (value) => _toggleApp(value, appInfo.packageName),
        isSelected: () => appList.selectedApps.contains(appInfo.packageName),
      ),
    );
  }

  @observable
  final ObservableList<UninstalledAppTile> uninstalledAppRows =
      ObservableList();

  @action
  Future<void> _buildUninstalledAppList() async {
    uninstalledAppRows.clear();
    final Set<String> allApps = await appList.getUninstalledApps();

    for (var packageName in allApps) {
      uninstalledAppRows.add(UninstalledAppTile(
        packageName: packageName,
        isSelected: () => appList.selectedApps.contains(packageName),
        onCheck: (value) => _toggleApp(value, packageName),
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
          builder: (_) =>
              appList.selectedApps.isEmpty || appList.uninstalledApps.isEmpty
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
    //showDialog(context: context, builder: (_) => const WarningDialog());
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
  Future<void> _uninstallApps() async {
    if (appList.selectedApps.isEmpty) {
      return;
    }

    final acceptUninstall = await showDialog(
        context: context,
        builder: (_) => UninstallDialog(packages: appList.selectedApps));

    if (!acceptUninstall || !await appList.kotlinBind.checkShizuku()) {
      return;
    }

    // Create a copy of selectedApps to avoid modifying it while iterating
    final List<String> appsToRemove = List.from(appList.selectedApps);

    for (var packageName in appsToRemove) {
      final sucess = await appList.uninstallApp(packageName);

      if (!sucess) {
        continue;
      }

      installedAppRows
          .removeWhere((element) => element.appInfo.packageName == packageName);
      uninstalledAppRows.add(UninstalledAppTile(
        packageName: packageName,
        isSelected: () => appList.selectedApps.contains(packageName),
        onCheck: (value) => _toggleApp(value, packageName),
      ));
    }
    appList.selectedApps.clear();
  }

  @action
  Future<void> _reinstallApps() async {
    for (var packageName in appList.selectedApps) {
      final appInfo = await appList.reinstallApp(packageName);

      if (appInfo == null) {
        continue;
      }

      uninstalledAppRows
          .removeWhere((element) => element.packageName == packageName);
      _addInstalledApp(appInfo);
    }
    appList.selectedApps.clear();
  }

  @action
  void clearSelectedApps() {
    appList.selectedApps.clear();
  }
}
