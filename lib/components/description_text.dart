import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class DescriptionText extends StatelessWidget {
  static final _linkRegExp = RegExp(r'https?://[^\s)\n]+');

  final String text;

  const DescriptionText({
    super.key,
    required this.text,
  });

  @override
  Widget build(BuildContext context) {
    // Parse text for links, build a list of RichTexts
    final descriptionTexts = <Widget>[];
    final matches = _linkRegExp.allMatches(text);

    var lastStart = 0;
    for (int i = 0; i < matches.length; ++i) {
      final match = matches.elementAt(i);
      final url = match.group(0);
      final preText = text.substring(lastStart, match.start);
      final end =
          i + 1 < matches.length ? matches.elementAt(i + 1).start : text.length;
      final postText = text.substring(match.end, end);

      descriptionTexts.add(RichText(
        text: TextSpan(
          text: preText,
          style: Theme.of(context).textTheme.bodySmall,
          children: [
            TextSpan(
              text: url,
              style: const TextStyle(color: Colors.blue),
              recognizer: TapGestureRecognizer()
                ..onTap = () => launchUrl(Uri.parse(url!)),
            ),
            TextSpan(
              text: postText,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ));

      lastStart = end;
    }

    if (matches.isEmpty) {
      descriptionTexts.add(Text(text));
    }

    return Column(
      children: descriptionTexts,
    );
  }
}
