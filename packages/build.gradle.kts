import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

/*
 * Device package model and operations shared by the Android app (in-process,
 * through Shizuku), the on-device ADB helper (via app_process) and the
 * desktop/web clients (decoding the helper's protobuf output).
 */
kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    android {
        namespace = "io.github.samolego.canta.packages"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("consumer-rules.pro"))
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    tasks.named("wasmJsBrowserTest") {
        enabled = false
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.protobuf)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.hiddenapibypass)
            compileOnly(project(":hiddenApiStubs"))
        }
    }
}
