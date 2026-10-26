package io.github.samolego.canta.packages

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FsPathTest {

    @Test
    fun refusesStorageRootsAndNormalizedAliases() {
        listOf(
            "/sdcard",
            "/storage/emulated",
            "/storage/emulated/0",
            "/storage/emulated/10/",
            "/storage/emulated/legacy",
            "/storage/self/primary",
            "/storage/1234-5678",
            "/data/media/0",
            "/data/media/10",
            "/data/local/tmp",
            "/storage//emulated/0/Download/..",
            "/sdcard/Download/..",
            "/system/app",
        ).forEach { assertFalse(isSafeDeletionPath(it), it) }
    }

    @Test
    fun permitsSubdirectoriesInsideStorageRoots() {
        listOf(
            "/sdcard/Download",
            "/storage/emulated/0/Download",
            "/storage/emulated/10/Pictures",
            "/storage/self/primary/Documents",
            "/storage/1234-5678/Music",
            "/data/media/0/Download",
            "/data/local/tmp/canta-cache",
        ).forEach { assertTrue(isSafeDeletionPath(it), it) }
    }
}
