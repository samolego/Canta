import 'dart:typed_data';

import 'package:installed_apps/app_info.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:mobx/mobx.dart';

import 'pair.dart';

class AppList {
  Future<List<Pair<AppInfo, bool>>> apps =
      Future<List<Pair<AppInfo, bool>>>.value([]);

  @observable
  ObservableSet<String> selectedApps = ObservableSet();

  AppList() {
    apps = _getAppList();
  }

  Future<List<Pair<AppInfo, bool>>> _getAppList() async {
    List<Pair<AppInfo, bool>> apps = [];
    var allApps = await InstalledApps.getInstalledApps(false, true, "");

    for (var app in allApps) {
      if (app.packageName == null) {
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
