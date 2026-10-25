package io.github.samolego.canta

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import io.github.samolego.canta.core.PresetRunDelegate
import io.github.samolego.canta.data.preset.CantaPresetData
import io.github.samolego.canta.ui.dialog.preset.PresetRunConfirmDialog
import io.github.samolego.canta.ui.theme.CantaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs a preset without showing Canta's UI — the "hidden" runner that pinned
 * launcher shortcuts target. Only the slide-to-confirm dialog is drawn;
 * toasts report progress. Orchestration lives in [PresetRunDelegate] so the
 * runner shares the app's Compose resources.
 *
 * Exported because launchers deliver pinned-shortcut intents under their own
 * identity. A foreign caller still needs an existing preset's UUID, the run
 * requires sliding the confirmation slider, and the "require authentication"
 * setting gates it behind the system credential prompt.
 */
class PresetRunActivity : FragmentActivity() {

    private companion object {
        /** How long prompts may stay unanswered before giving up. */
        const val AUTH_GRACE_MS = 120_000L
    }

    private val app: CantaApplication
        get() = application as CantaApplication

    private val delegate by lazy { PresetRunDelegate(app.dependencies, app.platform) }

    /**
     * Runs the preset outside the activity lifecycle: [finish] must not
     * abort a run that already started.
     */
    private val runScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Whether the preset run (not just a prompt) already started. */
    @Volatile
    private var started = false

    /** Loads the requested preset and opens confirmation, with a timeout for abandoned prompts. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uuid = intent
            ?.takeIf { it.action == PresetRunContract.ACTION_RUN_PRESET }
            ?.getStringExtra(PresetRunContract.EXTRA_PRESET_UUID)
        if (uuid.isNullOrEmpty()) {
            finish()
            return
        }

        // Neither the confirm dialog, Shizuku's permission dialog nor the
        // biometric prompt reports abandonment: this activity must not linger.
        lifecycleScope.launch {
            delay(AUTH_GRACE_MS)
            if (!started) finish()
        }
        lifecycleScope.launch {
            val preset = delegate.loadPreset(uuid)
            if (preset == null) {
                toast(delegate.presetNotFoundText())
                finish()
                return@launch
            }
            setContent {
                CantaTheme {
                    PresetRunConfirmDialog(
                        preset = preset,
                        onConfirm = { authorize(preset) },
                        onCancel = { finish() },
                    )
                }
            }
        }
    }

    /** Cancels pending setup while allowing an already-started run to finish. */
    override fun onDestroy() {
        super.onDestroy()
        if (!started) runScope.cancel()
    }

    /** Checks Shizuku and the authentication setting before starting the confirmed preset. */
    private fun authorize(preset: CantaPresetData) {
        lifecycleScope.launch {
            if (!delegate.ensureAuthorized()) {
                toast(delegate.unauthorizedText())
                finish()
                return@launch
            }
            delegate.requireBiometricIfNeeded(this@PresetRunActivity) { startRun(preset) }
        }
    }

    /** Executes the preset outside the activity lifecycle and reports its outcome on the main thread. */
    private fun startRun(preset: CantaPresetData) {
        started = true
        runScope.launch {
            toastOnMain(delegate.startedText(preset))
            toastOnMain(delegate.resultText(delegate.run(preset)))
            withContext(Dispatchers.Main) { finish() }
        }
    }

    /** Displays a runner message; callers must be on the main thread. */
    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /** Switches to the main thread before displaying a runner message. */
    private suspend fun toastOnMain(message: String) = withContext(Dispatchers.Main) { toast(message) }
}
