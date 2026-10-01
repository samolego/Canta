package io.github.samolego.canta.packages

import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Signature prefixes of the hidden framework APIs this module reflects on:
 * the package manager and installer internals, and `IntentSender`'s binder
 * constructor.
 */
private val HIDDEN_API_PREFIXES = arrayOf(
    "Landroid/content/pm/",
    "Landroid/content/IIntentSender",
    "Landroid/content/IntentSender;",
)

private val hiddenApisAllowed: Boolean by lazy { HiddenApiBypass.addHiddenApiExemptions(*HIDDEN_API_PREFIXES) }

/**
 * Lifts the hidden API restrictions for this module's reflection, so plain
 * `getMethod`/`getConstructor` calls reach them. Call before reflecting;
 * throws if the restrictions can't be lifted, as none of it would work then.
 */
internal fun allowHiddenApis() {
    check(hiddenApisAllowed) { "Hidden API restrictions could not be lifted" }
}
