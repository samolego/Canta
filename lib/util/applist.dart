import 'dart:collection';

import 'package:canta/util/app_info.dart';
import 'package:mobx/mobx.dart';

import '../kotlin_bind.dart';

class AppList {
  static SplayTreeSet<T> _createSortedSet<T extends Comparable>() {
    return SplayTreeSet<T>((a, b) => a.compareTo(b));
  }

  @observable
  ObservableSet<String> selectedApps = ObservableSet<String>.splayTreeSetFrom(
      [],
      compare: (a, b) => a.compareTo(b));

  Set<AppInfo> installedApps = _createSortedSet<AppInfo>();
  bool installedAppsLoaded = false;

  Set<String> uninstalledApps = _createSortedSet<String>();
  bool uninstalledAppsLoaded = false;

  KotlinBind kotlinBind = KotlinBind();

  AppList();

  Future<Set<AppInfo>> getInstalledApps() async {
    var apps = await kotlinBind.getInstalledApps();
    print("Loaded ${apps.length} apps!");
    installedApps.clear();
    installedApps.addAll(apps);
    installedAppsLoaded = true;

    return installedApps;
  }

  Future<Set<String>> getUninstalledApps() async {
    List<String> apps = await kotlinBind.getUninstalledApps();
    uninstalledApps.clear();
    uninstalledApps.addAll(apps);
    uninstalledAppsLoaded = true;

    return uninstalledApps;
  }

  Future<bool> uninstallApp(String packageName) async {
    final success = await kotlinBind.uninstallApp(packageName);

    if (!success) {
      return false;
    }

    installedApps.removeWhere((app) => packageName == app.packageName);
    uninstalledApps.add(packageName);

    return true;
  }

  Future<AppInfo?> reinstallApp(String packageName) async {
    final success = await kotlinBind.reinstallApp(packageName);

    if (!success) {
      return null;
    }

    uninstalledApps.remove(packageName);
    final info = await getAppInfo(packageName);
    installedApps.add(info);

    return info;
  }

  Future<AppInfo> getAppInfo(String packageName) async {
    return kotlinBind.getAppInfo(packageName);
  }
}
