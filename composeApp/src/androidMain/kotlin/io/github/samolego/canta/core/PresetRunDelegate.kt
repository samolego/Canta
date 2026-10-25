package io.github.samolego.canta.core

import androidx.fragment.app.FragmentActivity
import io.github.samolego.canta.data.preset.CantaPresetData
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.auth_required
import io.github.samolego.canta.generated.resources.auth_required_description
import io.github.samolego.canta.generated.resources.preset_not_found
import io.github.samolego.canta.generated.resources.preset_run_not_authorized
import io.github.samolego.canta.generated.resources.preset_run_started
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString

/**
 * Does the heavy lifting for `PresetRunActivity` (androidApp): preset lookup,
 * Shizuku/biometric gating and every user-facing message. Keeping it here
 * means the runner shares the app's Compose resources instead of duplicating
 * strings in Android resources.
 */
class PresetRunDelegate(
    private val deps: CantaAppDependencies,
    private val platform: AndroidCantaPlatform,
) {

    suspend fun loadPreset(uuid: String): CantaPresetData? =
        deps.presetStore.presetsFlow.first().find { it.uuid == uuid }

    suspend fun presetNotFoundText(): String = getString(Res.string.preset_not_found)

    suspend fun unauthorizedText(): String = getString(Res.string.preset_run_not_authorized)

    suspend fun startedText(preset: CantaPresetData): String =
        getString(Res.string.preset_run_started, preset.name)

    suspend fun resultText(result: PresetRunResult): String = result.describe()

    /** Asks Shizuku for permission when needed; reports whether we may proceed. */
    suspend fun ensureAuthorized(): Boolean = when (deps.handler.privilegeStatus) {
        PrivilegeStatus.ACTIVE -> true
        PrivilegeStatus.NOT_AUTHORIZED -> {
            deps.handler.requestAuthorization()
            deps.handler.isAuthorized
        }
        else -> false
    }

    /** Runs [onSuccess] immediately or after the system credential prompt. */
    suspend fun requireBiometricIfNeeded(activity: FragmentActivity, onSuccess: () -> Unit) {
        if (!deps.settings.authEnabledFlow.first()) {
            onSuccess()
            return
        }
        platform.requireBiometric(
            activity = activity,
            title = getString(Res.string.auth_required),
            subtitle = getString(Res.string.auth_required_description),
            onSuccess = onSuccess,
        )
    }

    suspend fun run(preset: CantaPresetData): PresetRunResult =
        PresetRunner(deps.handler).run(preset)
}
