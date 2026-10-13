# Hidden framework interfaces, compiled against :hiddenApiStubs (compileOnly).
# They exist at runtime in the framework but not in android.jar, so R8 would
# otherwise report them as missing classes.
-dontwarn android.content.pm.IPackageManager**
-dontwarn android.content.pm.IPackageInstaller**
