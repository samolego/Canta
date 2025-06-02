package org.samo_lego.canta.extension

import androidx.compose.runtime.snapshots.SnapshotStateMap

fun <T> mutableStateSetOf() = SnapshotStateMap<T, Unit>()

fun <T> SnapshotStateMap<T, Unit>.add(element: T) {
    this[element] = Unit
}

fun <T> SnapshotStateMap<T, Unit>.contains(element: T): Boolean {
    return this.containsKey(element)
}

fun <T> SnapshotStateMap<T, Unit>.addAll(elements: Collection<T>) {
    elements.forEach { this[it] = Unit }
}
