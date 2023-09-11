import 'package:canta/util/constants.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class WarningDialog extends StatelessWidget {
  const WarningDialog({super.key});

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text("Warning"),
      content: const Text(
          "This app is not responsible for any damage caused by the use of this app. Use at your own risk."),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(true),
          child: const Text("I understand"),
        ),
      ],
    );
  }
}

class UninstallDialog extends StatelessWidget {
  final Set<String> packages;

  const UninstallDialog({
    super.key,
    required this.packages,
  });

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text("Uninstall apps"),
      content: Text(
          "Do you really want to uninstall ${packages.length} selected app${packages.length > 1 ? "s" : ""}?"),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text("Cancel"),
        ),
        TextButton(
          onPressed: () => Navigator.of(context).pop(true),
          child: const Text("OK"),
        ),
      ],
    );
  }
}

class ShizukuDialog extends StatelessWidget {
  const ShizukuDialog({super.key});

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text("Shizuku not installed"),
      content: const Text(
          "This app requires Shizuku to work. Please install Shizuku and activate it first."),
      actions: [
        TextButton(
          onPressed: () => launchUrl(Constants.SHIZUKU_SITE),
          child: const Text("Visit site"),
        ),
        TextButton(
          onPressed: () => launchUrl(Constants.SHIZUKU_PLAYSTORE),
          child: const Text("Install Shizuku"),
        ),
      ],
    );
  }
}
