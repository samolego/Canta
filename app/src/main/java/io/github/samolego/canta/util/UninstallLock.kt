package io.github.samolego.canta.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.fragment.app.FragmentActivity
import io.github.samolego.canta.R

private const val TAG = "UninstallLock"

fun showBiometricPrompt(
    context: Context,
    onSuccess: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(context, R.string.auth_required))
        .setSubtitle(getString(context, R.string.auth_required_description))
        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()


    val biometricManager = BiometricManager.from(context)
    val authStatus = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    if (authStatus != BiometricManager.BIOMETRIC_SUCCESS) {
        LogUtils.e(TAG, "Device cannot use biometrics. Auth status: $authStatus")

    }

    val biometricPrompt = BiometricPrompt(context as FragmentActivity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                LogUtils.e(TAG, "An error occured while trying to authenticate. Code: $errorCode, message: $errString")
            }

            override fun onAuthenticationFailed() {
                LogUtils.w(TAG, "Authentication failed!")
            }
        })

    biometricPrompt.authenticate(promptInfo)
}
