package io.github.samolego.canta.ui.dialog

import android.content.Context
import io.github.samolego.canta.R
import io.github.samolego.canta.util.BloatData


fun cantaBloatData(context: Context): BloatData {
    return BloatData(
        installData = null,
        description = context.getString(R.string.canta_description, "Universal Debloater Alliance (https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)"),
        removal = null,
    )
}
