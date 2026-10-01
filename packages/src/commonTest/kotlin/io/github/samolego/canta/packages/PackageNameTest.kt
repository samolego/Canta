package io.github.samolego.canta.packages

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageNameTest {

    @Test
    fun acceptsRealPackageNames() {
        listOf("com.android.chrome", "io.github.samolego.canta", "com.miui.qr", "a.b", "com.x_y.Z9", "android")
            .forEach { assertTrue(isValidPackageName(it), it) }
    }

    @Test
    fun rejectsShellMetacharactersAndMalformedNames() {
        listOf(
            "", ".com.a", "com.a.", "com..a", "1com.a", "com.1a", "com.a b",
            "com.a;rm -rf /", "com.a && reboot", "\$(id).a", "com.a`id`", "com.a\nreboot", "com.a|sh",
        ).forEach { assertFalse(isValidPackageName(it), it) }
    }
}
