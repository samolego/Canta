# Hidden-API stubs (IPackageManager, IPackageInstaller, ...) are resolved at
# runtime against the framework through Shizuku binders; keep them intact.
-keep public class android.content.pm.** { *; }

# ShizukuProcess looks up the private, deprecated Shizuku.newProcess
# reflectively; R8 must not strip or rename it.
-keepclassmembers class rikka.shizuku.Shizuku {
    private static *** newProcess(java.lang.String[], java.lang.String[], java.lang.String);
}
# ShizukuRemoteProcess.waitForTimeout is also called reflectively.
-keepclassmembers class rikka.shizuku.ShizukuRemoteProcess {
    public boolean waitForTimeout(long, java.util.concurrent.TimeUnit);
}
