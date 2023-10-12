import 'package:canta/components/dialogues/shizuku_dialogues.dart';
import 'package:canta/components/dialogues/warning_dialogues.dart';
import 'package:canta/components/more_menu.dart';
import 'package:canta/components/tiles.dart';
import 'package:canta/search.dart';
import 'package:canta/util/applist.dart';
import 'package:canta/util/filters.dart';
import 'package:flutter/foundation.dart';
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
  Filter? _lastRemovalFilter;
  late final Future<void> _installedAppsLoading;
  late final Future<void> _uninstalledAppsLoading;

  @observable
  final ObservableSet<Filter> filters = ObservableSet();

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
      return filters.every((filter) => filter.shouldShow(appInfo));
    }).toSet();

    for (var app in filteredApps) {
      _addInstalledApp(app);
    }
  }

  @action
  void _addInstalledApp(AppInfo appInfo, {bool last = true}) {
    final InstalledAppTile tile = InstalledAppTile(
      appInfo: appInfo,
      onCheck: (value) => _toggleAppRemoval(value, appInfo.packageName),
      isSelected: () =>
          appList.selectedAppsForRemoval.contains(appInfo.packageName),
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
        isSelected: () =>
            appList.selectedAppsForInstallation.contains(packageName),
        onCheck: (value) => _toggleAppInstall(value, packageName),
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
            if (kDebugMode)
              IconButton(
                onPressed: () {
                  Navigator.pop(context);
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => const CantaApp(),
                    ),
                  );
                },
                icon: const Icon(Icons.refresh),
              ),
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
              icon: MoreMenu(
                  filters: filters,
                  clearSelectedApps: clearSelectedApps,
                  toggleFilter: _toggleFilter,
                  removalTypeFilter: _toggleRemovalTypeFilter,
                  selectedRemovalTypeFilter: _lastRemovalFilter),
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
                              itemBuilder: (_, index) {
                                if (index == installedAppRows.length - 1) {
                                  return Padding(
                                    padding:
                                        const EdgeInsets.only(bottom: 64.0),
                                    child: installedAppRows[index],
                                  );
                                }
                                return installedAppRows[index];
                              },
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
            Stack(
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
                                  itemBuilder: (_, index) {
                                    if (index ==
                                        uninstalledAppRows.length - 1) {
                                      return Padding(
                                        padding:
                                            const EdgeInsets.only(bottom: 64.0),
                                        child: uninstalledAppRows[index],
                                      );
                                    }
                                    return uninstalledAppRows[index];
                                  }),
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
                    if (appList.selectedAppsForInstallation.isEmpty) {
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
          ],
        ),
      ),
    );
  }

  @action
  void _toggleAppRemoval(bool? value, String packageName) {
    if (value!) {
      appList.selectedAppsForRemoval.add(packageName);
    } else {
      appList.selectedAppsForRemoval.remove(packageName);
    }
  }

  @action
  void _toggleAppInstall(bool? value, String packageName) {
    if (value!) {
      appList.selectedAppsForInstallation.add(packageName);
    } else {
      appList.selectedAppsForInstallation.remove(packageName);
    }
  }

  @action
  void _toggleRemovalTypeFilter(Filter? filter) {
    if (filter != _lastRemovalFilter) {
      filters.remove(_lastRemovalFilter);
      if (filter != null) {
        filters.add(filter);
      }

      _lastRemovalFilter = filter;
      _rescanApps();
    }
  }

  @action
  void _toggleFilter(bool? value, Filter filter) async {
    if (value!) {
      filters.add(filter);
    } else {
      filters.remove(filter);
    }

    _rescanApps();
  }

  @action
  void _rescanApps() {
    final Set<InstalledAppTile> filtered = {};
    for (var element in installedAppRows) {
      final visible =
          filters.every((filter) => filter.shouldShow(element.appInfo));

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
      final visible = filters.every((filter) => filter.shouldShow(element));

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
    if (appList.selectedAppsForRemoval.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("No apps selected."),
        ),
      );
      return;
    }

    final acceptUninstall = await showDialog(
        context: context,
        builder: (_) =>
            UninstallDialog(packages: appList.selectedAppsForRemoval));

    if (!acceptUninstall || await appList.kotlinBind.checkShizuku() != true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text("Uninstall cancelled."),
        ),
      );
      return;
    }

    // Create a copy of selectedApps to avoid modifying it while iterating
    final List<String> appsToRemove = List.from(appList.selectedAppsForRemoval);

    for (var packageName in appsToRemove) {
      final sucess = await appList.uninstallApp(packageName);

      if (!sucess) {
        continue;
      }

      installedAppRows
          .removeWhere((element) => element.appInfo.packageName == packageName);

      final tile = UninstalledAppTile(
        packageName: packageName,
        isSelected: () =>
            appList.selectedAppsForInstallation.contains(packageName),
        onCheck: (value) => _toggleAppInstall(value, packageName),
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
    appList.selectedAppsForRemoval.clear();

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text("Uninstall complete."),
      ),
    );
  }

  @action
  Future<void> _reinstallApps() async {
    for (var packageName in appList.selectedAppsForInstallation) {
      final appInfo = await appList.reinstallApp(packageName);

      if (appInfo == null) {
        continue;
      }

      uninstalledAppRows
          .removeWhere((element) => element.packageName == packageName);
      _addInstalledApp(appInfo, last: false);
    }
    appList.selectedAppsForInstallation.clear();

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text("Reinstall complete."),
      ),
    );
  }

  @action
  void clearSelectedApps() {
    appList.selectedAppsForRemoval.clear();
  }
}
