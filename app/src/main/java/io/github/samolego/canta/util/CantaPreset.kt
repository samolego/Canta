package io.github.samolego.canta.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CantaPreset(
        val name: String,
        val description: String,
        val createdDate: Long,
        val apps: Set<String>,
        val version: String = "1.0"
) : Parcelable