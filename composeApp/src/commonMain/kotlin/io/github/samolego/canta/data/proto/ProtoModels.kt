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
    @ProtoNumber(3) val disableRiskDialog: Boolean = false,
    @ProtoNumber(4) val latestBloatCommitHash: String = "",
    @ProtoNumber(5) val bloatListUrl: String = "",
    @ProtoNumber(6) val commitsUrl: String = "",
    @ProtoNumber(7) val allowUnsafeUninstalls: Boolean = false,
    @ProtoNumber(8) val hideSuccessDialog: Boolean = false,
    @ProtoNumber(9) val authEnabled: Boolean = false,
    // 10 (disable_adb_helper) was used by development builds: never reuse it.
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
