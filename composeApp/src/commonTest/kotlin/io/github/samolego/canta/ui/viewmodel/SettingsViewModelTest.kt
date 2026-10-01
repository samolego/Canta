package io.github.samolego.canta.ui.viewmodel

import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.testing.MemoryStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun disclaimerShowsUntilAcceptedForTheSession() = runTest {
        val settings = CantaSettings(MemoryStorage())
        val viewModel = SettingsViewModel(settings)
        assertEquals(true, viewModel.showRiskDialog.value)

        viewModel.acceptRiskDialog(neverShowAgain = false)

        assertEquals(false, viewModel.showRiskDialog.value)
        assertEquals(false, settings.disableRiskDialogFlow.first(), "only accepted for this session")
    }

    @Test
    fun neverShowAgainPersists() = runTest {
        val storage = MemoryStorage()
        val settings = CantaSettings(storage)
        SettingsViewModel(settings).acceptRiskDialog(neverShowAgain = true)
        // The store's state only changes once the write succeeded.
        settings.disableRiskDialogFlow.first { it }

        val reopened = SettingsViewModel(CantaSettings(storage))
        assertEquals(false, reopened.showRiskDialog.value)
    }

    @Test
    fun switchesReflectStoredValues() = runTest {
        val settings = CantaSettings(MemoryStorage())
        val viewModel = SettingsViewModel(settings)
        settings.setAutoUpdateBloatList(false)
        assertEquals(false, viewModel.autoUpdateBloatList.value)
    }
}
