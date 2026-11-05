package io.github.samolego.canta.util

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/** Constants for the opt-in package-safety telemetry (see [Telemetry]). */
object TelemetryConfig {
    const val BASE_URL = "https://aahwoixjjlopftjqnner.supabase.co/rest/v1/rpc/"

    /** Supabase publishable key; meant to be public. */
    const val PUBLISHABLE_KEY = "sb_publishable_2gyUfMGAKMro9MtNgVXxjg_uv8vxGy-"

    /** Proof-of-work difficulty: about this many hashes per event. */
    const val POW_K = 10_000L

    /** Most events the server accepts per request. */
    const val MAX_EVENTS = 250

    /** Quiet time after the last queued event before the batch is sent. */
    val DEBOUNCE = 30.seconds

    /** Pending events older than this are dropped rather than sent. */
    val PENDING_MAX_AGE = 30.days
}
