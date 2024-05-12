package org.samo_lego.canta.extension

import androidx.compose.runtime.snapshots.SnapshotStateMap

fun <T> mutableStateSetOf() = SnapshotStateMap<T, Unit>()

fun <T> mutableStateListOf(vararg elements: T) = SnapshotStateMap<T, Unit>().also {
    elements.forEach { element -> it[element] = Unit }
}

fun <T> SnapshotStateMap<T, Unit>.add(element: T) {
    this[element] = Unit
}
