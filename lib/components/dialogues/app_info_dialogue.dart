import 'package:canta/components/description_text.dart';
import 'package:canta/util/app_info.dart';
import 'package:flutter/material.dart';

class AppInfoDialogue extends StatelessWidget {
  final AppInfo appInfo;
  final VoidCallback onSelect;

  const AppInfoDialogue({
    super.key,
    required this.appInfo,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    const spacer = SizedBox(height: 16);
    return AlertDialog(
      title: ListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(
          appInfo.name,
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
        trailing: IconButton(
          onPressed: () => Navigator.of(context).pop(),
          icon: const Icon(Icons.close),
        ),
      ),
      titlePadding: const EdgeInsets.only(left: 16),
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
            DescriptionText(text: appInfo.description!),
          ] else
            const Text("No description provided"),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () {
            Navigator.of(context).pop();
            onSelect();
          },
          child: const Text("Select"),
        ),
      ],
    );
  }
}
