import 'dart:collection';
import 'dart:typed_data';

import 'package:installed_apps/app_info.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:mobx/mobx.dart';

import 'bootloop_removals.dart';
import 'pair.dart';

class AppList {
  Future<SplayTreeSet<Pair<AppInfo, bool>>> apps =
      Future<SplayTreeSet<Pair<AppInfo, bool>>>.value(SplayTreeSet((a, b) => a.left.name!.compareTo(b.left.name!)));

  @observable
  ObservableSet<String> selectedApps = ObservableSet();

  AppList() {
    apps = _getAppList();
  }

  Future<SplayTreeSet<Pair<AppInfo, bool>>> _getAppList() async {
    SplayTreeSet<Pair<AppInfo, bool>> apps = SplayTreeSet((a, b) => a.left.name!.compareTo(b.left.name!));
    var allApps = await InstalledApps.getInstalledApps(false, true, "");

    for (var app in allApps) {
      if (app.packageName == null || BOOTLOOPABLE_PACKAGES.contains(app.packageName)) {
        continue;
      }

      app.icon ??= Uint8List(0);
      app.name ??= app.packageName!;

      var systemApp = await InstalledApps.isSystemApp(app.packageName!);
      apps.add(Pair(app, systemApp!));
    }

    return apps;
  }

  static bool isSystemApp(Pair<AppInfo, bool> appInfo) {
    return appInfo.right;
  }
}
