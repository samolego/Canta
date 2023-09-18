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



class InstalledAppTile extends StatefulWidget {
  final AppInfo appInfo;
  final Function(bool?) onCheck;
  final bool Function() isSelected;

  const InstalledAppTile({
    super.key,
    required this.appInfo,
    required this.onCheck,
    required this.isSelected,
    required bool visible,
  });

  @override
  State<InstalledAppTile> createState() => _InstalledAppTileState();
}

class _InstalledAppTileState extends State<InstalledAppTile> {
  @override
  Widget build(BuildContext context) {
    return Observer(builder: (context) {
      return ListTile(
        leading: Image.memory(widget.appInfo.icon),
        title: Text(widget.appInfo.name),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(widget.appInfo.packageName),
            if (widget.appInfo.isSystemApp)
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
        trailing: Checkbox(
          value: widget.isSelected(),
          onChanged: (value) => widget.onCheck(value),
        ),
      );
    });
  }
}
