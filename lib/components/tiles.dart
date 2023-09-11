import 'package:canta/util/app_info.dart';
import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:mobx/mobx.dart';

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

class BoolWrap {
  @observable
  bool _value;

  BoolWrap(this._value);

  @action
  void setValue(bool value) => _value = value;

  bool get value => _value;
}

class InstalledAppTile extends StatefulWidget {
  final AppInfo appInfo;
  final Function(bool?) onCheck;
  final bool Function() isSelected;
  final BoolWrap visible;

  const InstalledAppTile({
    super.key,
    required this.appInfo,
    required this.onCheck,
    required this.isSelected,
    required this.visible,
  });

  @override
  State<InstalledAppTile> createState() => _InstalledAppTileState();
}

class _InstalledAppTileState extends State<InstalledAppTile> {
  @override
  Widget build(BuildContext context) {
    return Observer(builder: (context) {
      return Visibility(
        visible: widget.visible.value,
        child: ListTile(
          leading: Image.memory(widget.appInfo.icon),
          title: Text(widget.appInfo.name),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(widget.appInfo.packageName),
              if (widget.appInfo.isSystemApp)
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
          trailing: Checkbox(
            value: widget.isSelected(),
            onChanged: (value) => widget.onCheck(value),
          ),
        ),
      );
    });
  }
}
