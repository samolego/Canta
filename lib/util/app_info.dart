import 'dart:typed_data';

import 'package:flutter/material.dart';

class AppInfo extends Comparable<AppInfo> {
  String name;
  Uint8List icon;
  String packageName;
  bool isSystemApp;
  String? description;
  RemovalInfo? removalInfo;
  InstallInfo? installInfo;
  String? versionName;

  AppInfo(
    this.name,
    this.icon,
    this.packageName,
    this.isSystemApp, {
    this.description,
    this.removalInfo,
    this.installInfo,
    this.versionName
  });

  factory AppInfo.create(dynamic data) {
    final RemovalInfo? removalInfo;
    if (data["removal_info"] == null) {
      removalInfo = null;
    } else {
      removalInfo = RemovalInfo.values[data["removal_info"]];
    }
    final InstallInfo? installInfo;
    if (data["install_info"] == null) {
      installInfo = null;
    } else {
      installInfo = InstallInfo.values[data["install_info"]];
    }

    return AppInfo(
      data["name"],
      data["icon"],
      data["package_name"],
      data["is_system_app"],
      description: data["description"],
      removalInfo: removalInfo,
      installInfo: installInfo,
      versionName: data["version_name"]
    );
  }

  @override
  int compareTo(AppInfo other) {
    return name.compareTo(other.name);
  }

  @override
  String toString() {
    return "AppInfo{name=$name, packageName=$packageName, isSystemApp=$isSystemApp, appType=$removalInfo}";
  }
}

/// See <a href="https://github.com/0x192/universal-android-debloater/wiki/FAQ#how-are-the-recommendations-chosen>recommendations</a>
/// WARNING: Order must be synchronized with Kotlin code (org.samo_lego.canta.AppType)
enum RemovalInfo {
  RECOMMENDED(Icons.check, Colors.lightGreen),
  ADVANCED(Icons.settings, Colors.orange),
  EXPERT(Icons.warning_amber, Colors.red),
  UNSAFE(Icons.close, Colors.black);

  final IconData icon;
  final Color backgroundColor;

  const RemovalInfo(this.icon, this.backgroundColor);
}

enum InstallInfo { OEM, CARRIER }
