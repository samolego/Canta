import 'package:canta/app_info.dart';
import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';

class UninstalledAppTile extends ListTile {
  final String packageName;
  final Function(bool?) onCheck;
  final bool Function() isSelected;

  const UninstalledAppTile({
    super.key,
    required this.packageName,
    required this.onCheck,
    required this.isSelected,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      key: Key(packageName),
      title: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10.0),
        child: Text(packageName, style: const TextStyle(fontSize: 18)),
      ),
      trailing: Observer(
        builder: (_) => Checkbox(
          value: isSelected(),
          onChanged: (value) => onCheck(value),
        ),
      ),
    );
  }
}

class InstalledAppTile extends ListTile {
  final AppInfo appInfo;
  final Function(bool?) onCheck;
  final bool Function() isSelected;

  const InstalledAppTile({
    super.key,
    required this.appInfo,
    required this.onCheck,
    required this.isSelected,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Image.memory(appInfo.icon),
      title: Text(appInfo.name),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(appInfo.packageName),
          if (appInfo.isSystemApp)
            Badge(
              label: Row(
                children: const [
                  Icon(Icons.android),
                  Text("System"),
                ],
              ),
              backgroundColor: Colors.redAccent,
            ),
        ],
      ),
      trailing: Observer(
        builder: (context) => Checkbox(
          value: isSelected(),
          onChanged: (value) => onCheck(value),
        ),
      ),
    );

    return ListTile(
      key: Key(appInfo.packageName),
      trailing: Observer(
        builder: (_) => Checkbox(
          value: isSelected(),
          onChanged: (value) => onCheck(value),
        ),
      ),
      title: Row(
        children: [
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Image.memory(
              appInfo.icon,
              width: 48,
              height: 48,
            ),
          ),
          Text(appInfo.name, style: const TextStyle(fontSize: 22)),
          Text(appInfo.packageName),
          if (appInfo.isSystemApp)
            Badge(
              label: Row(
                children: const [
                  Icon(Icons.android),
                  SizedBox(width: 8.0),
                  Text("System App"),
                ],
              ),
              backgroundColor: Colors.redAccent,
            ),
        ],
      ),
    );
  }
}
