package io.github.samolego.canta.data.app

import io.github.samolego.canta.packages.PackageDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppInfoTest {

    @Test
    fun missingDetailsUseFallbacks() {
        val app = AppInfo(PackageDetails(packageName = "com.example.foo", installed = true))
        assertEquals("foo", app.name)
        assertEquals("unknown", app.versionName)
        assertNull(app.apkSize, "size 0 means unknown")
    }
}
