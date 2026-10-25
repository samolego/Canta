package io.github.samolego.canta.data.preset

data class CantaPresetData(
    val name: String,
    val description: String,
    val createdDate: Long,
    val apps: Set<String>,
    val version: String = "1.0",
    val uuid: String = "",
    /**
     * Device filesystem paths the preset deletes when it runs. Only paths
     * passing `isSafeDeletionPath` may be stored here; run targets normalize
     * them again before deleting.
     */
    val folders: Set<String> = emptySet(),
    /** Whether a preset run also uninstalls Canta itself, as its last action. */
    val removeCantaAfterRun: Boolean = false,
)
