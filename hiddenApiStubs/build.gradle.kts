plugins {
    alias(libs.plugins.android.library)
}

/*
 * Compile-time stand-ins for hidden framework interfaces (the pattern from
 * Shizuku's demo-hidden-api-stub). Only ever a `compileOnly` dependency: at
 * runtime the framework's real classes are used, so nothing here ships.
 */
android {
    namespace = "io.github.samolego.canta.hiddenapistubs"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }
}
