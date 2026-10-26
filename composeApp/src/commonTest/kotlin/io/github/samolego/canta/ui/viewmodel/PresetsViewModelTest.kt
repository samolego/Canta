package io.github.samolego.canta.ui.viewmodel

import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.data.preset.CantaPresetData
import io.github.samolego.canta.data.preset.CantaPresetStore
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.preset_run_done_failures
import io.github.samolego.canta.testing.FakeHandler
import io.github.samolego.canta.testing.FakePlatform
import io.github.samolego.canta.testing.MemoryStorage
import io.github.samolego.canta.testing.installedApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.getString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PresetsViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val storage = MemoryStorage()
    private val store = CantaPresetStore(storage)
    private val handler = FakeHandler(apps = installedApps("com.a"))
    private val platform = FakePlatform()
    // Lazy: viewModelScope picks its dispatcher on creation, which must come after setMain.
    private val viewModel by lazy {
        PresetsViewModel(store, handler, platform, CantaSettings(MemoryStorage()))
    }

    /** "success" or "error", whichever callback ran. */
    private fun import(json: String): List<String> {
        val outcome = mutableListOf<String>()
        viewModel.importFromJson(json, onSuccess = { outcome += "success" }, onError = { outcome += "error" })
        return outcome
    }

    @Test
    fun importKeepsOnlyAppsOnThisDevice() = runTest {
        assertEquals(listOf("success"), import("""{"name": "P", "apps": ["com.a", "com.not.here"]}"""))

        assertEquals(listOf(setOf("com.a")), store.presetsFlow.first().map { it.apps })
    }

    @Test
    fun unreadableJsonIsReportedAndNothingSaved() = runTest {
        assertEquals(listOf("error"), import("""{"apps": ["com.a"]}"""))
        assertEquals(listOf("error"), import("   "))

        assertTrue(store.presetsFlow.first().isEmpty())
        assertEquals(0, storage.writes)
    }

    @Test
    fun failedSaveIsReported() = runTest {
        storage.failWrites = true

        assertEquals(listOf("error"), import("""{"name": "P", "apps": ["com.a"]}"""))
        assertTrue(store.presetsFlow.first().isEmpty())
    }

    @Test
    fun missingFolderAccessDoesNotBlockAppUninstalls() = runTest {
        // Cache the resource before launching the view model, as other view-model tests do.
        val expected = getString(Res.string.preset_run_done_failures, 1, 0, 1)
        platform.needsAllFilesAccess = true
        handler.failing += "/sdcard/Download"
        val preset = CantaPresetData(
            name = "Cleanup",
            description = "",
            createdDate = 0,
            apps = setOf("com.a"),
            folders = setOf("/sdcard/Download"),
        )

        viewModel.runPreset(preset)

        assertEquals(listOf(listOf("com.a") to false), handler.uninstallCalls)
        assertEquals(listOf(listOf("/sdcard/Download")), handler.deletePathsCalls)
        assertEquals(listOf(expected), platform.shownMessages)
        assertFalse(viewModel.runningPreset)
    }
}
