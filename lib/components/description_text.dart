import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class DescriptionText extends StatelessWidget {
  static final _linkRegExp = RegExp(r'https?://[^\s)]+');

  final String text;

  const DescriptionText({
    super.key,
    required this.text,
  });

  @override
  Widget build(BuildContext context) {
    // Parse text for links, build a list of TextSpans

    final descriptionTexts = <Widget>[];
    final matches = _linkRegExp.allMatches(text);

    for (final match in matches) {
      final url = match.group(0);
      if (kDebugMode) {
        print("Detected url: $url");
      }

      final start = match.start;
      final end = match.end;
      descriptionTexts.add(RichText(
        text: TextSpan(
          text: text.substring(0, start),
          style: const TextStyle(color: Colors.black),
          children: [
            TextSpan(
              text: url,
              style: const TextStyle(color: Colors.blue),
              recognizer: TapGestureRecognizer()
                ..onTap = () => launchUrl(Uri.parse(url!)),
            ),
            TextSpan(
              text: text.substring(end),
              style: const TextStyle(color: Colors.black),
            ),
          ],
        ),
      ));
    }

    if (matches.isEmpty) {
      descriptionTexts.add(Text(text));
    }

    return Column(
      children: descriptionTexts,
    );
  }
}
