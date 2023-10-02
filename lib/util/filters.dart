import 'package:canta/util/app_info.dart';

class Filter {
  final String name;
  final bool Function(AppInfo) shouldShow;

  Filter(this.name, this.shouldShow);

  static final List<Filter> _filters = [];

  static List<Filter> get availableFilters {
    if (_filters.isEmpty) {
      _filters.addAll([
        Filter("Hide system", (app) => !app.isSystemApp),
        Filter("Hide user", (app) => app.isSystemApp),
      ]);

      for (var value in RemovalInfo.values) {
        final name =
            value.toString().split(".")[1].replaceAll("_", " ").toLowerCase();
        _filters.add(Filter("Hide $name", (app) => app.removalInfo != value));
      }
    }

    return _filters;
  }
}
