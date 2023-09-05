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
}

/// See <a href="https://github.com/0x192/universal-android-debloater/wiki/FAQ#how-are-the-recommendations-chosen>recommendations</a>
enum AppType { RECOMMENDED, ADVANCED, EXPERT, UNSAFE, UNKNOWN }
