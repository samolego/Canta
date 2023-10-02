import 'package:canta/kotlin_bind.dart';
import 'package:canta/util/constants.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class ShizukuNotInstalledDialog extends StatelessWidget {
  const ShizukuNotInstalledDialog({super.key});

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text("Shizuku not installed"),
      content: const Text(
        "This app requires Shizuku to work. Please install Shizuku and activate it first.",
      ),
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

class ShizukuNotActiveDialog extends StatelessWidget {
  final KotlinBind kotlinBind;

  const ShizukuNotActiveDialog(this.kotlinBind, {super.key});

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text("Shizuku not active"),
      content: const Text(
        "This app requires Shizuku to work. Please activate Shizuku first.",
      ),
      actions: [
        TextButton(
          onPressed: () => launchUrl(Constants.SHIZUKU_SITE),
          child: const Text("Visit guide"),
        ),
        TextButton(
          onPressed: () {
            // Launch shizuku app
            kotlinBind.launchShizuku();
          },
          child: const Text("Activate Shizuku"),
        ),
      ],
    );
  }
}
