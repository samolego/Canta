package io.github.samolego.canta

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import io.github.samolego.canta.ui.CantaApp

class MainActivity : FragmentActivity() {

    private val app: CantaApplication
        get() = application as CantaApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)

        app.platform.attachActivity(this)

        setContent {
            CantaApp(deps = app.dependencies, closeApp = { finishAndRemoveTask() })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        app.platform.detachActivity(this)
    }
}
