# Hidden-API stubs (IPackageManager, IPackageInstaller, ...) are resolved at
# runtime against the framework through Shizuku binders; keep them intact.
-keep public class android.content.pm.** { *; }

# WorkManager's Room (2.6.x) ships `-keep class * extends RoomDatabase` without
# a member spec, so R8 full mode strips WorkDatabase_Impl's no-arg constructor,
# which Room instantiates reflectively -> crash in InitializationProvider.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
