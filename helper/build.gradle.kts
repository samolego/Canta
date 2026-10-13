plugins {
    alias(libs.plugins.android.application)
}

/*
 * On-device ADB helper. Not a real app: its release APK is only a dex
 * container, run by desktop/web Canta as the shell user through
 *   CLASSPATH=<apk> app_process /system/bin io.github.samolego.canta.helper.Main <command>
 * It is never installed, so it has no manifest entries, resources or signing
 * requirements.
 */
android {
    namespace = "io.github.samolego.canta.helper"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.samolego.canta.helper"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // app_process doesn't verify signatures; sign with the debug key
            // only so the APK is a well-formed, reproducible artifact.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    implementation(project(":packages"))
}

/*
 * Exposes the release APK to other projects (composeApp bundles it for the
 * desktop and web targets). Consumers select it by this Usage attribute.
 */
val adbHelperApk = configurations.create("adbHelperApk") {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, "canta-adb-helper-apk"))
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        artifacts.add(adbHelperApk.name, variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK))
    }
}
