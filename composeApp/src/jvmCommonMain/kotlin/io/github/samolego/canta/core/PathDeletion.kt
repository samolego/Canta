package io.github.samolego.canta.core

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Deletes an already validated, resolved target without traversing descendant symlinks.
 * A symlink at [path] is rejected; links inside a directory are unlinked without
 * touching their targets. Missing targets are a no-op; filesystem errors propagate.
 */
internal fun deleteWithoutFollowingLinks(path: Path) {
    require(!Files.isSymbolicLink(path)) { "Deletion target is a symbolic link" }
    if (!Files.exists(path, NOFOLLOW_LINKS)) return
    Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
        /** Unlinks a file or symlink without visiting a symlink's target. */
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.delete(file)
            return FileVisitResult.CONTINUE
        }

        /** Deletes an emptied directory, propagating errors encountered during its traversal. */
        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            if (exc != null) throw exc
            Files.delete(dir)
            return FileVisitResult.CONTINUE
        }
    })
}
