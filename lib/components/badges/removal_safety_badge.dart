import 'package:canta/components/badges/colored_badge.dart';
import 'package:canta/util/app_info.dart';
import 'package:flutter/material.dart';

class RemovalSafetyBadge extends StatelessWidget {
  final RemovalInfo removalInfo;

  const RemovalSafetyBadge({
    super.key,
    required this.removalInfo,
  });

  @override
  Widget build(BuildContext context) {
    final text = removalInfo.toString().split(".")[1].replaceAll("_", " ");
    final label = text[0] + text.substring(1).toLowerCase();

    return ColoredBadge(
      backgroundColor: removalInfo.backgroundColor,
      icon: removalInfo.icon,
      label: label,
    );
  }
}
