package io.github.samolego.canta

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import io.github.samolego.canta.ui.CantaApp

class MainActivity : FragmentActivity() {

    private val app: CantaApplication
        get() = application as CantaApplication

    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            app.platform.onFolderPicked(uri)
        }

    private val storagePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)

        app.platform.attachActivity(this)
        app.platform.folderPickerLauncher = { folderPicker.launch(null) }
        app.platform.storagePermissionLauncher = {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            CantaApp(deps = app.dependencies, closeApp = { finishAndRemoveTask() })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        app.platform.detachActivity(this)
        app.platform.folderPickerLauncher = null
        app.platform.storagePermissionLauncher = null
    }
}
