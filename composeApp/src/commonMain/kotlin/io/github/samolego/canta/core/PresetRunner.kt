package io.github.samolego.canta.core

import io.github.samolego.canta.CANTA_PACKAGE_NAME
import io.github.samolego.canta.data.preset.CantaPresetData
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.preset_run_done
import io.github.samolego.canta.generated.resources.preset_run_done_failures
import io.github.samolego.canta.packages.isValidPackageName
import io.github.samolego.canta.util.LogUtils
import org.jetbrains.compose.resources.getString

/** Outcome of running one preset end to end. */
data class PresetRunResult(
    val appsRemoved: Int = 0,
    val appsFailed: Int = 0,
    val foldersRemoved: Int = 0,
    val foldersFailed: Int = 0,
    val cantaRemoved: Boolean = false,
) {
    /** Operations that did not succeed (apps + folders). */
    val failures: Int get() = appsFailed + foldersFailed
}

/**
 * Executes a preset end to end against [handler]: uninstalls its apps,
 * deletes its filesystem paths, then — only when the preset asks for it —
 * uninstalls Canta itself. Self-removal always runs last: it can kill the
 * process, and Canta's package is filtered out of the plain app list so only
 * [CantaPresetData.removeCantaAfterRun] can remove it.
 */
class PresetRunner(private val handler: CantaHandler) {

    private companion object {
        const val TAG = "PresetRunner"
    }

    /** Removes matching apps, deletes folders, then attempts explicit self-removal; returns operation counts. */
    suspend fun run(preset: CantaPresetData): PresetRunResult {
        var appsRemoved = 0
        var appsFailed = 0
        var foldersRemoved = 0
        var foldersFailed = 0
        var cantaRemoved = false

        val apps = preset.apps
            .filter { it != CANTA_PACKAGE_NAME && isValidPackageName(it) }
            .filter { runCatching { handler.packageExists(it) }.getOrDefault(false) }
        if (apps.isNotEmpty()) {
            handler.uninstallApps(apps).collect { result ->
                if (result.success) appsRemoved++ else {
                    appsFailed++
                    LogUtils.w(TAG, "Uninstalling '${result.packageName}' failed: ${result.message}")
                }
            }
        }

        if (preset.folders.isNotEmpty()) {
            handler.deletePaths(preset.folders.toList()).collect { result ->
                if (result.success) foldersRemoved++ else {
                    foldersFailed++
                    LogUtils.w(TAG, "Deleting '${result.packageName}' failed: ${result.message}")
                }
            }
        }

        if (preset.removeCantaAfterRun) {
            handler.uninstallApps(listOf(CANTA_PACKAGE_NAME)).collect { result ->
                if (result.success) cantaRemoved = true
                else LogUtils.w(TAG, "Self-uninstall failed: ${result.message}")
            }
        }

        return PresetRunResult(appsRemoved, appsFailed, foldersRemoved, foldersFailed, cantaRemoved)
    }
}

/** Localized one-line summary of a completed run. */
suspend fun PresetRunResult.describe(): String =
    if (failures == 0) {
        getString(Res.string.preset_run_done, appsRemoved, foldersRemoved)
    } else {
        getString(Res.string.preset_run_done_failures, appsRemoved, foldersRemoved, failures)
    }
