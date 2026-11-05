package io.github.samolego.canta.ui.viewmodel

import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.data.proto.PendingUninstallProto
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
    fun onboardingCompletionPersistsChoice() = runTest {
        val storage = MemoryStorage()
        val settings = CantaSettings(storage)
        SettingsViewModel(settings).completeOnboarding(contribute = true)
        settings.onboardingVersionFlow.first { it == CURRENT_ONBOARDING_VERSION }

        val reopened = SettingsViewModel(CantaSettings(storage))
        assertEquals(CURRENT_ONBOARDING_VERSION, reopened.onboardingVersion.first { it != null })
        assertEquals(true, settings.analyticsEnabledFlow.first())
    }

    @Test
    fun disablingAnalyticsClearsPendingReboot() = runTest {
        val settings = CantaSettings(MemoryStorage())
        settings.setAnalyticsEnabled(true)
        settings.setPendingReboot(listOf(PendingUninstallProto(pkg = "a.b")))
        val viewModel = SettingsViewModel(settings)

        viewModel.setAnalyticsEnabled(false)
        settings.pendingRebootFlow.first { it.isEmpty() }

        assertEquals(false, settings.analyticsEnabledFlow.first())
    }

    @Test
    fun switchesReflectStoredValues() = runTest {
        val settings = CantaSettings(MemoryStorage())
        val viewModel = SettingsViewModel(settings)
        settings.setAutoUpdateBloatList(false)
        assertEquals(false, viewModel.autoUpdateBloatList.value)
    }
}
