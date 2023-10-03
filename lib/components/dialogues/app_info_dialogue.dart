import 'package:canta/util/app_info.dart';
import 'package:flutter/material.dart';

class AppInfoDialogue extends StatelessWidget {
  final AppInfo appInfo;

  const AppInfoDialogue({
    super.key,
    required this.appInfo,
  });

  @override
  Widget build(BuildContext context) {
    const spacer = SizedBox(height: 16);
    return AlertDialog(
      title: Text(appInfo.name),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            leading: Image.memory(appInfo.icon),
            title: Text(appInfo.packageName),
          ),
          spacer,
          if (appInfo.description != null &&
              appInfo.description!.isNotEmpty) ...[
            Text(appInfo.description!),
          ] else
            const Text("No description provided"),
        ],
      ),
    );
  }
}
