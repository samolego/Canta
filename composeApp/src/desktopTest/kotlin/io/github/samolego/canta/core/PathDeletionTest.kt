package io.github.samolego.canta.core

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PathDeletionTest {

    @Test
    fun deletesTheTreeWithoutFollowingDescendantLinks() {
        val root = Files.createTempDirectory("canta-deletion-test")
        try {
            val outside = Files.createDirectory(root.resolve("outside"))
            val sentinel = Files.writeString(outside.resolve("keep.txt"), "keep")
            val target = Files.createDirectory(root.resolve("target"))
            Files.createDirectory(target.resolve("nested"))
            Files.writeString(target.resolve("nested/remove.txt"), "remove")
            Files.createSymbolicLink(target.resolve("link"), outside)
            Files.createSymbolicLink(target.resolve("broken-link"), root.resolve("missing"))

            deleteWithoutFollowingLinks(target)

            assertFalse(Files.exists(target))
            assertEquals("keep", Files.readString(sentinel))
        } finally {
            deleteWithoutFollowingLinks(root)
        }
    }

    @Test
    fun refusesASymlinkAsTheDeletionTarget() {
        val root = Files.createTempDirectory("canta-deletion-test")
        try {
            val outside = Files.createDirectory(root.resolve("outside"))
            val sentinel = Files.writeString(outside.resolve("keep.txt"), "keep")
            val link = Files.createSymbolicLink(root.resolve("link"), outside)

            assertFailsWith<IllegalArgumentException> { deleteWithoutFollowingLinks(link) }

            assertTrue(Files.isSymbolicLink(link))
            assertEquals("keep", Files.readString(sentinel))
        } finally {
            deleteWithoutFollowingLinks(root)
        }
    }
}
