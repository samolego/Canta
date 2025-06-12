package io.github.samolego.canta.util

import android.os.Handler
import android.os.Looper
import android.widget.Toast


fun Toast.showFor(duration: Long) {
    this.show()
    // Force cancel after duration has passed
    Handler(Looper.getMainLooper()).postDelayed({
        this.cancel()
    }, duration)
}
