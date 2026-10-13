@file:OptIn(ExperimentalSerializationApi::class)

package io.github.samolego.canta.packages

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Bumped whenever the helper's commands or messages change incompatibly.
 * The helper artifact is re-pushed by checksum anyway; this is reported by
 * the helper's `version` command for diagnostics.
 */
const val PROTOCOL_VERSION = 3

/*
 * Wire format between the on-device ADB helper and Canta, encoded with
 * kotlinx-serialization-protobuf. Never renumber or reuse a field number.
 *
 * All defaults are proto3 zero values on purpose: zero-valued fields are
 * omitted when encoding, so any other default would change their meaning.
 */

/** Everything Canta knows about a single package on the device. */
@Serializable
data class PackageDetails(
    @ProtoNumber(1) val packageName: String = "",
    /** User-visible app label, or empty when unknown. */
    @ProtoNumber(2) val label: String = "",
    /** Version name, or empty when unknown. */
    @ProtoNumber(3) val versionName: String = "",
    @ProtoNumber(4) val versionCode: Long = 0,
    /** Whether the app is disabled. Negative so a zero-valued message means "enabled". */
    @ProtoNumber(5) val disabled: Boolean = false,
    /** Size of the base APK plus all split APKs in bytes, or 0 when unknown. */
    @ProtoNumber(6) val apkSize: Long = 0,
    @ProtoNumber(7) val isSystem: Boolean = false,
    /** Whether the package is installed for the current user. */
    @ProtoNumber(8) val installed: Boolean = false,
    /** Whether this is a system app with an update installed over it. */
    @ProtoNumber(9) val hasSystemUpdate: Boolean = false,
)

/** Same rule as `ApplicationInfo.canResetToFactory`, for packages read through the helper. */
val PackageDetails.canResetToFactory: Boolean
    get() = isSystem && hasSystemUpdate

/** The name to show: the app's label, else the last segment of its package name. */
val PackageDetails.displayName: String
    get() = label.ifEmpty { packageName.substringAfterLast('.') }

/**
 * The order Canta lists apps in: installed apps first, then uninstalled ones,
 * each alphabetically (case-insensitive) by [displayName].
 */
fun List<PackageDetails>.inDisplayOrder(): List<PackageDetails> =
    sortedWith(compareBy({ !it.installed }, { it.displayName.lowercase() }))

@Serializable
data class PackageDetailsList(
    @ProtoNumber(1) val protocolVersion: Int = 0,
    @ProtoNumber(2) val packages: List<PackageDetails> = emptyList(),
)

/** One icon, streamed by the helper as its own message. */
@Serializable
data class AppIcon(
    @ProtoNumber(1) val packageName: String = "",
    /** PNG-encoded icon. */
    @ProtoNumber(2) val png: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean =
        other is AppIcon && packageName == other.packageName && png.contentEquals(other.png)

    override fun hashCode(): Int = 31 * packageName.hashCode() + png.contentHashCode()
}
