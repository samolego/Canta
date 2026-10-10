package io.github.samolego.canta.ui.viewmodel

import io.github.samolego.canta.CANTA_PACKAGE_NAME
import io.github.samolego.canta.core.CantaDevice
import io.github.samolego.canta.core.DeviceConnectionFailure
import io.github.samolego.canta.core.DeviceDiscovery
import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.auth_required
import io.github.samolego.canta.generated.resources.auth_required_description
import io.github.samolego.canta.generated.resources.cannot_uninstall_canta
import io.github.samolego.canta.generated.resources.device_no_longer_available
import io.github.samolego.canta.testing.FakeHandler
import io.github.samolego.canta.testing.FakePlatform
import io.github.samolego.canta.testing.MemoryStorage
import io.github.samolego.canta.testing.offlineBloatRepository
import io.github.samolego.canta.ui.AppAction
import io.github.samolego.canta.ui.AppsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val handler = FakeHandler()
    private val platform = FakePlatform()
    private val settings = CantaSettings(MemoryStorage())

    /**
     * Compose resources read a string file on an IO thread the first time, so
     * the view model's coroutines would finish at an unpredictable moment.
     * Cached strings resolve synchronously, keeping the view model on the
     * test dispatcher.
     */
    private fun runVmTest(body: suspend TestScope.() -> Unit): TestResult = runTest {
        listOf(
            Res.string.cannot_uninstall_canta,
            Res.string.auth_required,
            Res.string.auth_required_description,
            Res.string.device_no_longer_available,
        ).forEach { getString(it) }
        body()
    }

    private suspend fun mainViewModel(): Pair<MainViewModel, AppListViewModel> {
        val appList = AppListViewModel(handler, offlineBloatRepository())
        val main = MainViewModel(handler, platform, settings, appList)
        return main to appList
    }

    @Test
    fun cantaNeverUninstallsItself() = runVmTest {
        val (main, appList) = mainViewModel()
        appList.selectedApps.addAll(listOf("com.a", CANTA_PACKAGE_NAME))

        main.applyToSelected(AppsType.INSTALLED)

        assertEquals(listOf(getString(Res.string.cannot_uninstall_canta)), platform.shownMessages)
        assertTrue(handler.uninstallCalls.isEmpty())
        assertNull(main.dialog)
    }

    @Test
    fun uninstallIsConfirmedFirstAndReportsSuccess() = runVmTest {
        handler.resettable += "com.a"
        val (main, appList) = mainViewModel()
        appList.selectedApps.add("com.a")

        main.applyToSelected(AppsType.INSTALLED)
        assertEquals(MainDialog.ConfirmUninstall(appCount = 1, canResetToFactory = true), main.dialog)
        assertTrue(handler.uninstallCalls.isEmpty(), "nothing happens before confirming")

        main.onUninstallConfirmed(resetToFactory = true)
        assertEquals(listOf(listOf("com.a") to true), handler.uninstallCalls)
        assertEquals(MainDialog.Success(count = 1, action = AppAction.UNINSTALL), main.dialog)
    }

    @Test
    fun disableAppReportsDisableSuccess() = runVmTest {
        val (main, appList) = mainViewModel()
        appList.selectedApps.add("com.a")

        main.applyToSelected(AppsType.INSTALLED)
        assertEquals(MainDialog.ConfirmUninstall(appCount = 1, canResetToFactory = false), main.dialog)

        main.onUninstallConfirmed(disableApp = true, uninstallApp = false)
        assertEquals(listOf(listOf("com.a")), handler.disableCalls)
        assertEquals(MainDialog.Success(count = 1, action = AppAction.DISABLE), main.dialog)
    }

    @Test
    fun reinstallSkipsConfirmationAndHiddenSuccessDialogStaysHidden() = runVmTest {
        settings.setHideSuccessDialog(true)
        val (main, appList) = mainViewModel()
        appList.selectedApps.add("com.a")

        main.applyToSelected(AppsType.UNINSTALLED)

        assertEquals(listOf(listOf("com.a")), handler.reinstallCalls)
        assertNull(main.dialog)
    }

    @Test
    fun nothingIsAppliedUntilBiometricsSucceed() = runVmTest {
        settings.setConfirmBeforeUninstall(false)
        settings.setAuthEnabled(true)
        val (main, appList) = mainViewModel()
        appList.selectedApps.add("com.a")

        main.applyToSelected(AppsType.INSTALLED)
        assertTrue(platform.biometricRequested)
        assertTrue(handler.uninstallCalls.isEmpty())

        platform.approveBiometric()
        assertEquals(listOf(listOf("com.a") to false), handler.uninstallCalls)
    }

    @Test
    fun failedConnectionIsExplainedAndTheChooserStaysOpen() = runVmTest {
        val device = CantaDevice(id = "serial", displayName = "Phone")
        val adbHandler = FakeHandler(authorized = false, deviceDiscovery = DeviceDiscovery.List).apply {
            devices = listOf(device)
            connectFailure = DeviceConnectionFailure.DeviceUnavailable
        }
        val appList = AppListViewModel(adbHandler, offlineBloatRepository())
        val main = MainViewModel(adbHandler, platform, settings, appList)
        assertEquals(MainDialog.DeviceChooser(devices = listOf(device)), main.dialog, "opens with discovered devices")

        main.selectDevice(device)

        assertEquals(listOf(getString(Res.string.device_no_longer_available)), platform.shownMessages)
        assertIs<MainDialog.DeviceChooser>(main.dialog)
        assertTrue(appList.apps.isEmpty(), "no apps without a device")

        adbHandler.connectFailure = null
        main.selectDevice(device)

        assertNull(main.dialog, "chooser closes once connected")
        assertFalse(appList.apps.isEmpty(), "apps load after connecting")
    }
}
