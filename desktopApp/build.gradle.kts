plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    jvm("desktop")

    sourceSets {
        named("desktopMain").dependencies {
            implementation(project(":composeApp"))
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.samolego.canta.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "canta"
            packageVersion = providers.gradleProperty("version_name").get()
            linux {
                iconFile.set(project.file("icon.png"))
            }
        }
    }
}
