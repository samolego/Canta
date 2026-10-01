# Hidden-API stubs (IPackageManager, IPackageInstaller, ...) are resolved at
# runtime against the framework through Shizuku binders; keep them intact.
-keep public class android.content.pm.** { *; }
