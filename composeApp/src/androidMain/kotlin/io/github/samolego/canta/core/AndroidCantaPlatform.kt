package io.github.samolego.canta.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import io.github.samolego.canta.PresetRunContract
import io.github.samolego.canta.util.LogUtils

class AndroidCantaPlatform(
    private val context: Context,
) : CantaPlatform {

    companion object {
        private const val TAG = "AndroidCantaPlatform"
    }

    @Volatile
    private var biometricActivity: FragmentActivity? = null

    /** Wired by `MainActivity` to its OpenDocumentTree result launcher. */
    var folderPickerLauncher: (() -> Unit)? = null

    /** Wired by `MainActivity` to its WRITE_EXTERNAL_STORAGE launcher (< API 30). */
    var storagePermissionLauncher: (() -> Unit)? = null

    private var folderCallback: ((String?) -> Unit)? = null

    /** Attach the activity (from `onCreate`); used for biometric prompts. */
    fun attachActivity(activity: FragmentActivity) {
        biometricActivity = activity
    }

    /** Detach the activity when it is destroyed. */
    fun detachActivity(activity: FragmentActivity) {
        if (biometricActivity === activity) {
            biometricActivity = null
        }
    }

    override fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun requireBiometric(title: String, subtitle: String, onSuccess: () -> Unit) {
        val activity = biometricActivity
        if (activity == null) {
            // Fail closed: skipping authentication would bypass the setting.
            LogUtils.e(TAG, "No FragmentActivity attached; cannot authenticate")
            return
        }
        requireBiometric(activity, title, subtitle, onSuccess)
    }

    /** Authenticates with [activity] without changing the main activity's registered owner. */
    fun requireBiometric(activity: FragmentActivity, title: String, subtitle: String, onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(context)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    LogUtils.e(
                        TAG,
                        "Biometric error. Code: $errorCode, message: $errString"
                    )
                }

                override fun onAuthenticationFailed() {
                    LogUtils.w(TAG, "Biometric authentication failed!")
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    override fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Canta", text))
    }

    override fun readClipboard(): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).text?.toString()
    }

    override val supportsPresetShortcuts: Boolean
        get() = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    /**
     * Pins a launcher shortcut that runs the preset headlessly through
     * `PresetRunActivity`. The system asks the user to confirm the pin.
     */
    override fun installPresetShortcut(presetName: String, presetUuid: String): Boolean {
        if (!supportsPresetShortcuts) return false
        val intent = Intent(PresetRunContract.ACTION_RUN_PRESET)
            .setClassName(context.packageName, PresetRunContract.RUN_ACTIVITY_CLASS)
            .putExtra(PresetRunContract.EXTRA_PRESET_UUID, presetUuid)
        return try {
            val shortcut = ShortcutInfoCompat.Builder(context, PresetRunContract.SHORTCUT_ID_PREFIX + presetUuid)
                .setShortLabel(presetName)
                .setLongLabel("Canta: $presetName")
                .setIcon(IconCompat.createWithResource(context, context.applicationInfo.icon))
                .setIntent(intent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        } catch (e: Exception) {
            // e.g. IllegalStateException when called without a foreground activity
            LogUtils.e(TAG, "Failed to pin preset shortcut", e)
            false
        }
    }

    override val needsAllFilesAccess: Boolean
        get() = !hasAllFilesAccess()

    /** Whether the app may read/write all of shared storage directly. */
    private fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${context.packageName}".toUri(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            storagePermissionLauncher?.invoke()
        }
    }

    override val supportsFolderPicker: Boolean
        get() = folderPickerLauncher != null

    override fun pickFolder(onPicked: (String?) -> Unit) {
        val launcher = folderPickerLauncher ?: return onPicked(null)
        folderCallback = onPicked
        launcher()
    }

    /** Called by `MainActivity` when the document-tree picker returns. */
    fun onFolderPicked(uri: Uri?) {
        folderCallback?.invoke(uri?.let(::treeUriToPath))
        folderCallback = null
    }

    /**
     * Maps a `content://com.android.externalstorage.documents/tree/<vol>:<rel>`
     * URI back to the filesystem path the preset stores. Other providers have
     * no stable mapping — null means "type the path manually".
     */
    private fun treeUriToPath(uri: Uri): String? {
        if (uri.authority != "com.android.externalstorage.documents") return null
        val split = DocumentsContract.getTreeDocumentId(uri).split(':', limit = 2)
        val base = if (split[0] == "primary") "/sdcard" else "/storage/${split[0]}"
        val relative = split.getOrElse(1) { "" }
        return if (relative.isEmpty()) base else "$base/$relative"
    }
}