import 'package:canta/util/app_info.dart';
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
    return InkWell(
      onTap: () => onCheck(!isSelected()),
      child: ListTile(
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
      ),
    );
  }
}


class InstalledAppTile extends StatelessWidget {
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
    return InkWell(
      onTap: () => onCheck(!isSelected()),
      child: ListTile(
        leading: Image.memory(appInfo.icon),
        title: Text(appInfo.name),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(appInfo.packageName),
            if (appInfo.isSystemApp)
              const Badge(
                label: Row(
                  children: [
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
                )),
      ),
    );
  }
}
