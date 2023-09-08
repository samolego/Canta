import 'package:flutter/material.dart';
import 'package:flutter_mobx/flutter_mobx.dart';

class InstallableAppTile extends Row {
  final String packageName;
  final Function(bool?, String) onCheck;
  final bool Function() isSelected;

  InstallableAppTile({
    super.key,
    required this.packageName,
    required this.onCheck,
    required this.isSelected,
  });

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
          onChanged: (value) => onCheck(value, packageName),
        ),
      ),
    );
  }
}
