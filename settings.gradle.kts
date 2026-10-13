pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // PREFER_SETTINGS (not FAIL_ON_PROJECT_REPOS): the Kotlin Wasm plugin adds
    // its Binaryen distribution repository programmatically, which fails the
    // build under FAIL_ON_PROJECT_REPOS (:composeApp:kotlinWasmBinaryenSetup).
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Binaryen (wasm-opt) distribution used by the Kotlin Wasm plugin.
        // Mirrors the plugin's own Ivy layout (see BinaryenSetupTask in
        // kotlin-gradle-plugin sources): artifacts like
        // version_125/binaryen-version_125-x86_64-linux.tar.gz.
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
            name = "Binaryen distributions"
            patternLayout {
                artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
            }
            metadataSources { artifact() }
            content { includeGroup("com.github.webassembly") }
        }
        // Node.js distribution used by the Kotlin Wasm/JS plugin
        // (see NodeJsSetupTask in kotlin-gradle-plugin sources): artifacts
        // like v25.0.0/node-v25.0.0-linux-x64.tar.gz.
        ivy("https://nodejs.org/dist") {
            name = "Node.js distributions"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources { artifact() }
            content { includeGroup("org.nodejs") }
        }
        // Yarn distribution used by the Kotlin Wasm/JS plugin
        // (see YarnSetupTask in kotlin-gradle-plugin sources): artifacts
        // like v1.22.17/yarn-v1.22.17.tar.gz.
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "Yarn distributions"
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources { artifact() }
            content { includeGroup("com.yarnpkg") }
        }
    }
}

rootProject.name = "Canta"
include(":hiddenApiStubs")
include(":packages")
include(":helper")
include(":composeApp")
include(":androidApp")
include(":desktopApp")
include(":webApp")
