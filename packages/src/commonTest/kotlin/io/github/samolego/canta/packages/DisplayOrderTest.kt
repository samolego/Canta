package io.github.samolego.canta.packages

import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayOrderTest {

    @Test
    fun displayNameFallsBackToLastPackageSegment() {
        assertEquals("Chrome", PackageDetails(packageName = "com.android.chrome", label = "Chrome").displayName)
        assertEquals("chrome", PackageDetails(packageName = "com.android.chrome").displayName)
        assertEquals("android", PackageDetails(packageName = "android").displayName)
    }

    @Test
    fun installedFirstThenUninstalledEachAlphabetically() {
        val packages = listOf(
            PackageDetails(packageName = "z.gone", label = "Alpha", installed = false),
            PackageDetails(packageName = "b.app", label = "beta", installed = true),
            PackageDetails(packageName = "a.app", label = "Zeta", installed = true),
            PackageDetails(packageName = "c.app", label = "Alpha", installed = true),
            PackageDetails(packageName = "y.gone", label = "Omega", installed = false),
        )
        assertEquals(
            listOf("c.app", "b.app", "a.app", "z.gone", "y.gone"),
            packages.inDisplayOrder().map { it.packageName },
        )
    }
}
