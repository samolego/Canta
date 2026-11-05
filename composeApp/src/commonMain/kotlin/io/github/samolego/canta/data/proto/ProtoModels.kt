@file:OptIn(ExperimentalSerializationApi::class)

package io.github.samolego.canta.data.proto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/*
 * Protobuf schema for persisted settings and presets, encoded with
 * kotlinx-serialization-protobuf. These mirror the former `settings.proto`
 * and `presets.proto` (proto3) and must stay byte-compatible with the
 * `app_settings.pb` / `presets.pb` files written by earlier Android releases:
 * never renumber or reuse a field number.
 *
 * All defaults are proto3 zero values on purpose: proto3 omits zero-valued
 * fields when encoding, so any other default would change the meaning of
 * a stored `false` / `""`.
 */

@Serializable
data class AppSettingsProto(
    @ProtoNumber(1) val autoUpdateBloatList: Boolean = false,
    @ProtoNumber(2) val confirmBeforeUninstall: Boolean = false,
    // 3 (disableRiskDialog) is unused now; kept so old files still decode.
    @ProtoNumber(3) val disableRiskDialog: Boolean = false,
    @ProtoNumber(4) val latestBloatCommitHash: String = "",
    @ProtoNumber(5) val bloatListUrl: String = "",
    @ProtoNumber(6) val commitsUrl: String = "",
    @ProtoNumber(7) val allowUnsafeUninstalls: Boolean = false,
    @ProtoNumber(8) val hideSuccessDialog: Boolean = false,
    @ProtoNumber(9) val authEnabled: Boolean = false,
    // 10 (disable_adb_helper) was used by development builds: never reuse it.
    @ProtoNumber(11) val analyticsEnabled: Boolean = false,
    @ProtoNumber(12) val onboardingVersion: Int = 0,
    @ProtoNumber(13) val pendingReboot: List<PendingUninstallProto> = emptyList(),
)

/**
 * A system-package uninstall awaiting its "did the phone reboot fine" report.
 * [cohortWeek] is the ISO date of that week's Monday; [batchBucket] is
 * "1", "2-10" or "11+".
 */
@Serializable
data class PendingUninstallProto(
    @ProtoNumber(1) val pkg: String = "",
    @ProtoNumber(2) val bootId: String = "",
    @ProtoNumber(3) val cohortWeek: String = "",
    @ProtoNumber(4) val batchBucket: String = "",
    @ProtoNumber(5) val uninstalledAt: Long = 0,
    /** Device context at uninstall time; blank in entries from before it was recorded. */
    @ProtoNumber(6) val brand: String = "",
    @ProtoNumber(7) val rom: String = "",
    @ProtoNumber(8) val romVersion: Int = 0,
    @ProtoNumber(9) val sdk: Int = 0,
    /** Whether `survived_reboot` was already reported; the entry then waits for a possible reinstall. */
    @ProtoNumber(10) val survived: Boolean = false,
    /** `Build.FINGERPRINT` at uninstall time; stored locally only, never sent. Blank if unknown. */
    @ProtoNumber(11) val buildFingerprint: String = "",
    /** Which device the uninstall happened on (see `CantaHandler.deviceKey`); stored locally only, never sent. Blank if unknown. */
    @ProtoNumber(12) val deviceKey: String = "",
)

/**
 * Whether the pending packages were uninstalled right when the device finished booting; stored
 * locally by the boot receiver until the post-boot check delivers its reports. Never sent.
 */
@Serializable
data class BootSnapshotProto(
    @ProtoNumber(1) val bootId: String = "",
    @ProtoNumber(2) val buildFingerprint: String = "",
    @ProtoNumber(3) val uninstalled: List<String> = emptyList(),
    @ProtoNumber(4) val installed: List<String> = emptyList(),
)

@Serializable
data class CantaPresetProto(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val description: String = "",
    @ProtoNumber(3) val createdDate: Long = 0,
    @ProtoNumber(4) val apps: List<String> = emptyList(),
    @ProtoNumber(5) val version: String = "",
    @ProtoNumber(6) val uuid: String = "",
)

@Serializable
data class PresetsListProto(
    @ProtoNumber(1) val presets: List<CantaPresetProto> = emptyList(),
)
