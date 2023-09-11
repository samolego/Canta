import 'dart:typed_data';

class AppInfo extends Comparable<AppInfo> {
  String name;
  Uint8List icon;
  String packageName;
  bool isSystemApp;
  AppType appType;

  AppInfo(
    this.name,
    this.icon,
    this.packageName,
    this.isSystemApp,
    this.appType,
  );

  factory AppInfo.create(dynamic data) {
    return AppInfo(
      data["name"],
      data["icon"],
      data["package_name"],
      data["is_system_app"],
      AppType.values[data["app_type"]],
    );
  }

  @override
  int compareTo(AppInfo other) {
    return name.compareTo(other.name);
  }

  @override
  String toString() {
    return "AppInfo{name=$name, packageName=$packageName, isSystemApp=$isSystemApp, appType=$appType}";
  }
}

/// See <a href="https://github.com/0x192/universal-android-debloater/wiki/FAQ#how-are-the-recommendations-chosen>recommendations</a>
/// WARNING: Order must be synchronized with Kotlin code (org.samo_lego.canta.AppType)
enum AppType { RECOMMENDED, ADVANCED, EXPERT, UNSAFE, UNKNOWN }
