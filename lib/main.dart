import 'package:canta/applist.dart';
import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:mobx/mobx.dart';

import 'app_info.dart';

void main() {
  runApp(const CantaApp());
}

class CantaApp extends StatelessWidget {
  const CantaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Canta',
      theme: ThemeData(
        primarySwatch: Colors.orange,
      ),
      home: const HomePage(title: 'Canta'),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.title});

  final String title;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final AppList appList = AppList();

  List<Function(AppInfo)> filters = [];

  _HomePageState();

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
                            getBadgeElement(app.isSystemApp),
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
  final ObservableList<Row> uninstalledAppRows = ObservableList<Row>();

  @action
  Future<void> _buildUninstalledAppList() async {
    uninstalledAppRows.clear();
    final Set<String> allApps = await appList.getUninstalledApps();

    for (var packageName in allApps) {
      uninstalledAppRows.add(_constructUninstallRow(packageName));
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

  Row _constructUninstallRow(String packageName) {
    return Row(
      key: Key(packageName),
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Flexible(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const SizedBox(width: 10),
                  Flexible(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 10.0),
                          child: Text(packageName,
                              style: const TextStyle(fontSize: 18)),
                        ),
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
            value: appList.selectedApps.contains(packageName),
            onChanged: (value) => _toggleApp(value, packageName),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: DefaultTabController(
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
          ),
          body: TabBarView(
            children: [
              Scaffold(
                body: Center(
                  // Center is a layout widget. It takes a single child and positions it
                  // in the middle of the parent.
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
                // Center is a layout widget. It takes a single child and positions it
                // in the middle of the parent.
                child: _getUninstalledAppList(),
              ),
            ],
          ),
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
  void _uninstallApps() {
    // Create a copy of selectedApps to avoid modifying it while iterating
    final List<String> appsToRemove = List.from(appList.selectedApps);
    for (var packageName in appsToRemove) {
      installedAppRows
          .removeWhere((element) => element.key == Key(packageName));
      uninstalledAppRows.add(_constructUninstallRow(packageName));

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

  getBadgeElement(bool systemApp) {
    if (systemApp) {
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
    } else {
      return const SizedBox();
    }
  }
}
