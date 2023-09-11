import 'package:canta/util/app_info.dart';

class Filter {
  final String name;
  final bool Function(AppInfo) filterFn;

  Filter(this.name, this.filterFn);

  static List<Filter> available = [
    Filter("System apps only", (app) => app.isSystemApp),
    Filter("User apps only", (app) => !app.isSystemApp),
  ];
}