import 'package:canta/components/dialogues/shizuku_dialogues.dart';
import 'package:canta/components/dialogues/warning_dialogues.dart';
import 'package:canta/components/tiles.dart';
import 'package:canta/search.dart';
import 'package:canta/util/applist.dart';
import 'package:canta/util/filters.dart';
import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:mobx/mobx.dart';

import 'util/app_info.dart';

void main() {
  runApp(const CantaApp());
}

class CantaApp extends StatelessWidget {
  static const String title = 'Canta';

  const CantaApp({
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: title,
      theme: ThemeData(
        primarySwatch: Colors.orange,
        primaryColorDark: Colors.deepOrange,
      ),
      home: HomePage(title: title),
    );
  }
}

class HomePage extends StatefulWidget {
  final Set<AppInfo> filteredHiddenApps = {};

  HomePage({
    super.key,
    required this.title,
  });

  final String title;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final AppList appList = AppList();
  late final Future<void> _installedAppsLoading;
  late final Future<void> _uninstalledAppsLoading;

  @observable
  final ObservableSet<Function(AppInfo)> filters = ObservableSet();

  @observable
  final ObservableList<InstalledAppTile> installedAppRows = ObservableList();

  @observable
  final ObservableList<UninstalledAppTile> uninstalledAppRows =
      ObservableList();

  @override
  void initState() {
    super.initState();
    appList.kotlinBind.checkShizuku().then((value) {
      final Widget? dialog;
      if (value == null) {
        // Shizuku is not even installed
        dialog = const ShizukuNotInstalledDialog();
      } else if (!value) {
        dialog = ShizukuNotActiveDialog(appList.kotlinBind);
      } else {
        // Shizuku is installed and active
        dialog = null;
      }

      if (dialog != null) {
        showDialog(context: context, builder: (_) => dialog!);
      }
    });

    _installedAppsLoading = _buildInstalledAppList();
    _uninstalledAppsLoading = _buildUninstalledAppList();
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
  void _addInstalledApp(AppInfo appInfo, {bool last = true}) {
    final InstalledAppTile tile = InstalledAppTile(
      appInfo: appInfo,
      onCheck: (value) => _toggleApp(value, appInfo.packageName),
      isSelected: () => appList.selectedApps.contains(appInfo.packageName),
    );

    if (last) {
      installedAppRows.add(tile);
      return;
    }

    // find appropriate index to insert app
    int index = 0;
    while (index < installedAppRows.length &&
        appInfo.name.compareTo(installedAppRows[index].appInfo.name) >= 0) {
      index += 1;
    }

    if (index == installedAppRows.length) {
      installedAppRows.add(tile);
      return;
    }

    installedAppRows.insert(index, tile);
  }

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
                  final List<PopupMenuItem<void>> items = Filter
                      .availableFilters
                      .map((fltr) => PopupMenuItem<void>(
                              child: Observer(
                            builder: (_) => Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(fltr.name),
                                Checkbox(
                                  value: filters.contains(fltr.shouldShow),
                                  onChanged: (value) =>
                                      _toggleFilter(value, fltr),
                                ),
                              ],
                            ),
                          )))
                      .toList();
                  items.add(
                    PopupMenuItem<String>(
                      child: const Text("Deselect all apps"),
                      onTap: () => clearSelectedApps(),
                    ),
                  );
                  return items;
                },
              ),
            ),
          ],
        ),
        body: TabBarView(
          children: [
            Stack(
              children: [
                Center(
                  child: FutureBuilder<void>(
                      future: _installedAppsLoading,
                      builder: (context, snapshot) {
                        if (snapshot.connectionState == ConnectionState.done) {
                          return Observer(
                            builder: (_) => ListView.builder(
                              padding:
                                  const EdgeInsets.symmetric(vertical: 8.0),
                              itemCount: installedAppRows.length,
                              itemBuilder: (_, index) =>
                                  installedAppRows[index],
                            ),
                          );
                        }
                        return const Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text("Loading apps ..."),
                            SizedBox(height: 16.0),
                            CircularProgressIndicator(),
                          ],
                        );
                      }),
                ),
                Positioned(
                  bottom: 16.0,
                  right: 16.0,
                  child: FloatingActionButton(
                    backgroundColor: Colors.red,
                    onPressed: _uninstallApps,
                    tooltip: 'Uninstall',
                    child: const Icon(
                      Icons.delete,
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
            Center(
              child: Stack(
                children: [
                  FutureBuilder<void>(
                      future: _uninstalledAppsLoading,
                      builder: (context, snapshot) {
                        if (snapshot.connectionState == ConnectionState.done) {
                          return Observer(
                            builder: (_) => uninstalledAppRows.isEmpty
                                ? const Center(
                                    child: Padding(
                                      padding: EdgeInsets.all(16.0),
                                      child: Text(
                                          "No recoverable uninstalled apps found.",
                                          style: TextStyle(fontSize: 18)),
                                    ),
                                  )
                                : ListView.builder(
                                    itemCount: uninstalledAppRows.length,
                                    itemBuilder: (_, index) =>
                                        uninstalledAppRows[index]),
                          );
                        }
                        return const Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text("Loading apps ..."),
                            SizedBox(height: 16.0),
                            CircularProgressIndicator(),
                          ],
                        );
                      }),
                  Observer(
                    builder: (_) {
                      if (appList.selectedApps.isEmpty ||
                          appList.uninstalledApps.isEmpty) {
                        return const Offstage();
                      }

                      return Positioned(
                        bottom: 16.0,
                        right: 16.0,
                        child: FloatingActionButton(
                          backgroundColor: Colors.green,
                          onPressed: _reinstallApps,
                          tooltip: 'Reinstall apps.',
                          child: const Icon(
                            Icons.install_mobile,
                            color: Colors.white,
                          ),
                        ),
                      );
                    },
                  ),
                ],
              ),
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
      filters.add(filter.shouldShow);
    } else {
      filters.remove(filter.shouldShow);
    }

    // Rescan apps
    final Set<InstalledAppTile> filtered = {};
    for (var element in installedAppRows) {
      final visible = filters.every((filter) => filter(element.appInfo));

      if (!visible) {
        filtered.add(element);
      }
    }

    for (var element in filtered) {
      installedAppRows.remove(element);
    }

    // Check old filtered apps
    final Set<AppInfo> hidden = filtered.map((e) => e.appInfo).toSet();
    for (var element in widget.filteredHiddenApps) {
      final visible = filters.every((filter) => filter(element));

      if (visible) {
        _addInstalledApp(element, last: false);
      } else {
        hidden.add(element);
      }
    }

    widget.filteredHiddenApps.clear();
    widget.filteredHiddenApps.addAll(hidden);
  }

  @action
  Future<void> _uninstallApps() async {
    if (appList.selectedApps.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("No apps selected."),
        ),
      );
      return;
    }

    final acceptUninstall = await showDialog(
        context: context,
        builder: (_) => UninstallDialog(packages: appList.selectedApps));

    if (!acceptUninstall || await appList.kotlinBind.checkShizuku() != true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("Uninstall cancelled."),
        ),
      );
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

      final tile = UninstalledAppTile(
        packageName: packageName,
        isSelected: () => appList.selectedApps.contains(packageName),
        onCheck: (value) => _toggleApp(value, packageName),
      );

      int index = 0;
      while (index < uninstalledAppRows.length &&
          packageName.compareTo(uninstalledAppRows[index].packageName) >= 0) {
        index += 1;
      }

      if (index == uninstalledAppRows.length) {
        uninstalledAppRows.add(tile);
      } else {
        uninstalledAppRows.insert(index, tile);
      }
    }
    appList.selectedApps.clear();

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text("Uninstall complete."),
      ),
    );
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
      _addInstalledApp(appInfo, last: false);
    }
    appList.selectedApps.clear();

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text("Reinstall complete."),
      ),
    );
  }

  @action
  void clearSelectedApps() {
    appList.selectedApps.clear();
  }
}
