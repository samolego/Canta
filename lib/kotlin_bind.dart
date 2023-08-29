import 'package:canta/pair.dart';
import 'package:flutter/services.dart';
import 'package:mobx/mobx.dart';

class KotlinBind {
  static const platform = MethodChannel('org.samo_lego.canta/native');
  @observable
  Pair<List<String>, bool> _uninstalledApps = Pair([], false);

  Future<void> uninstallApp(String packageName) async {
    // Call relevant Kotlin method
    try {
      await platform.invokeMethod('uninstallApp', {'packageName': packageName});
      _uninstalledApps.left.add(packageName);
    } on PlatformException {}
  }

  Future<List<String>> getUninstalledApps() async {
    if (_uninstalledApps.right) return Future.value(_uninstalledApps.left);

    // Call relevant Kotlin method
    try {
      List<Object?> apps = await platform.invokeMethod('getUninstalledApps');
      List<String> appList = [];
      for (var app in apps) {
        appList.add(app as String);
      }
      _uninstalledApps = Pair(appList, true);
      return Future.value(appList);
    } on PlatformException {
      return Future.value([]);
    }
  }

  Future<List<String>> getInstalledApps() async {
    // Call relevant Kotlin method
    try {
      List<Object?> apps = await platform.invokeMethod('getInstalledApps');
      List<String> appList = [];
      for (var app in apps) {
        appList.add(app as String);
      }
      return Future.value(appList);
    } on PlatformException {
      return Future.value([]);
    }
  }
}
