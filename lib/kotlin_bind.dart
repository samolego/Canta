import 'package:flutter/services.dart';

import 'app_info.dart';

class KotlinBind {
  static const platform = MethodChannel('org.samo_lego.canta/native');

  Future<void> uninstallApp(String packageName) async {
    // Call relevant Kotlin method
    try {
      await platform.invokeMethod('uninstallApp', {'packageName': packageName});
    } on PlatformException {}
  }

  Future<List<String>> getUninstalledApps() async {
    // Call relevant Kotlin method
    try {
      List<Object?> apps = await platform.invokeMethod('getUninstalledApps');
      return Future.value(apps.map((app) => app as String).toList());
    } on PlatformException {
      return Future.value([]);
    }
  }

  Future<List<AppInfo>> getInstalledApps() async {
    // Call relevant Kotlin method
    try {
      List<Object?> apps = await platform.invokeMethod('getInstalledAppsInfo');
      List<AppInfo> appInfoList =
          apps.map((app) => AppInfo.create(app)).toList();
      return Future.value(appInfoList);
    } on PlatformException {
      return Future.value([]);
    }
  }

  void reinstallApp(String packageName) {
    // Call relevant Kotlin method
    try {
      platform.invokeMethod('reinstallApp', {'packageName': packageName});
    } on PlatformException {}
  }
}
