import 'package:flutter/material.dart';

class ColoredBadge extends StatelessWidget {
  final Color backgroundColor;
  final IconData icon;
  final String label;

  const ColoredBadge({
    super.key,
    required this.backgroundColor,
    required this.icon,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    const spacer = SizedBox(width: 8);

    return IntrinsicWidth(
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: Material(
          color: backgroundColor,
          child: Row(
            children: [
              spacer,
              Icon(
                icon,
                color: Colors.white,
                size: 16,
              ),
              const SizedBox(width: 4),
              Text(
                label,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w400,
                  fontSize: 12,
                ),
              ),
              spacer,
            ],
          ),
        ),
      ),
    );
  }
}
