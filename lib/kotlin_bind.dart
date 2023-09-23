import 'package:flutter/services.dart';

import 'util/app_info.dart';

class KotlinBind {
  static const platform = MethodChannel('org.samo_lego.canta/native');

  Future<bool> uninstallApp(String packageName) async {
    // Call relevant Kotlin method
    try {
      return await platform
          .invokeMethod('uninstallApp', {'packageName': packageName});
    } on PlatformException {}
    return false;
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

  Future<bool> reinstallApp(String packageName) async {
    // Call relevant Kotlin method
    try {
      return await platform
          .invokeMethod('reinstallApp', {'packageName': packageName});
    } on PlatformException {
      return false;
    }
  }

  Future<AppInfo> getAppInfo(String packageName) async {
    try {
      return AppInfo.create(await platform
          .invokeMethod('getAppInfo', {'packageName': packageName}));
    } on PlatformException {
      return AppInfo(
          packageName.split(".").last,
          Uint8List.fromList(List.generate(48 * 48 * 3, (index) => 0)),
          packageName,
          true,
          AppType.UNKNOWN);
    }
  }

  Future<bool> checkShizuku() async {
    try {
      return await platform.invokeMethod('checkShizuku');
    } on PlatformException {
      return false;
    }
  }
}
