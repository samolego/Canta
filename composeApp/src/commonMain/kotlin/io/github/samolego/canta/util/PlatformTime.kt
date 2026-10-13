package io.github.samolego.canta.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Current epoch time in milliseconds, platform-independent (Kotlin 2.3 stdlib Clock). */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()