import 'package:canta/components/badges/colored_badge.dart';
import 'package:flutter/material.dart';

class SystemBadge extends StatelessWidget {
  const SystemBadge({super.key});

  @override
  Widget build(BuildContext context) {
    return const ColoredBadge(
      backgroundColor: Colors.red,
      icon: Icons.android,
      label: "System",
    );
  }
}
