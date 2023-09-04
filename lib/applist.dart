
import 'package:canta/app_info.dart';
import 'package:mobx/mobx.dart';

import 'kotlin_bind.dart';

class AppList {
  static _createObservableSortedSet<T extends Comparable>() {
    return ObservableSet<T>.splayTreeSetFrom([],
        compare: (a, b) => a.compareTo(b));
  }

  @observable
  ObservableSet<String> selectedApps = _createObservableSortedSet<String>();

  @observable
  ObservableSet<AppInfo> installedApps = _createObservableSortedSet<AppInfo>();
  @observable
  bool installedAppsLoaded = false;

  @observable
  ObservableSet<String> uninstalledApps = _createObservableSortedSet<String>();
  @observable
  bool uninstalledAppsLoaded = false;

  KotlinBind kotlinBind = KotlinBind();

  AppList() {
    getInstalledApps();
    getUninstalledApps();
  }

  @action
  void getInstalledApps() async {
    var apps = await kotlinBind.getInstalledApps();
    print("Loaded ${apps.length} apps!");
    installedApps.addAll(apps);
    installedAppsLoaded = true;
  }

  @action
  void getUninstalledApps() async {
    List<String> apps = await kotlinBind.getUninstalledApps();
    uninstalledApps.addAll(apps);
    uninstalledAppsLoaded = true;
  }

  @action
  void uninstallApp(String packageName) {
    kotlinBind.uninstallApp(packageName);

    if (installedAppsLoaded) {
      installedApps.removeWhere((app) => packageName == app.packageName);
    }

    if (uninstalledAppsLoaded) {
      uninstalledApps.add(packageName);
    }
  }

  @action
  void reinstallApp(String packageName) {
    kotlinBind.reinstallApp(packageName);

    // Todo: improve this
    installedAppsLoaded = false;
    getInstalledApps();

    if (uninstalledAppsLoaded) {
      uninstalledApps.remove(packageName);
    }
  }
}
