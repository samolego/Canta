import 'dart:collection';

import 'package:canta/util/app_info.dart';
import 'package:flutter/foundation.dart';
import 'package:mobx/mobx.dart';

import '../kotlin_bind.dart';

class AppList {
  static SplayTreeSet<T> _createSortedSet<T extends Comparable>() {
    return SplayTreeSet<T>((a, b) => a.compareTo(b));
  }

  @observable
  ObservableSet<String> selectedAppsForRemoval =
      ObservableSet<String>.splayTreeSetFrom([],
          compare: (a, b) => a.compareTo(b));

  @observable
  ObservableSet<String> selectedAppsForInstallation =
      ObservableSet<String>.splayTreeSetFrom([],
          compare: (a, b) => a.compareTo(b));

  Set<AppInfo> installedApps = _createSortedSet<AppInfo>();

  Set<String> uninstalledApps = _createSortedSet<String>();

  KotlinBind kotlinBind = KotlinBind();

  AppList();

  Future<Set<AppInfo>> getInstalledApps() async {
    var apps = await kotlinBind.getInstalledApps();
    if (kDebugMode) {
      print("Loaded ${apps.length} apps!");
    }
    installedApps.clear();
    installedApps.addAll(apps);

    return installedApps;
  }

  Future<Set<String>> getUninstalledApps() async {
    List<String> apps = await kotlinBind.getUninstalledApps();
    uninstalledApps.clear();
    uninstalledApps.addAll(apps);

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
