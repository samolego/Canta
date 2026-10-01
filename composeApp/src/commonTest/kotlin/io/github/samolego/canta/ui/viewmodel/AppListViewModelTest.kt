package io.github.samolego.canta.ui.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import io.github.samolego.canta.data.app.AppBadgeInfo
import io.github.samolego.canta.generated.resources.Res
import io.github.samolego.canta.generated.resources.canta_description
import io.github.samolego.canta.testing.FakeHandler
import io.github.samolego.canta.testing.offlineBloatRepository
import io.github.samolego.canta.ui.AppsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {

    private val icon = ImageBitmap(1, 1)
    private val handler = FakeHandler()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** Caches the string loading uses, so [AppListViewModel.loadApps] runs without leaving the test dispatcher. */
    private fun runVmTest(body: suspend TestScope.() -> Unit): TestResult = runTest {
        getString(Res.string.canta_description)
        body()
    }

    private suspend fun viewModel() = AppListViewModel(handler, offlineBloatRepository())

    private fun AppListViewModel.hasIcon() = apps.associate { it.packageName to (it.icon != null) }

    @Test
    fun uninstallUpdatesOnlyTheAppsThatSucceeded() = runVmTest {
        handler.failing += "com.b"
        val viewModel = viewModel()
        viewModel.loadApps().join()
        viewModel.selectedApps.addAll(listOf("com.a", "com.b"))

        assertEquals(1, viewModel.applyToSelected(AppsType.INSTALLED))

        assertEquals(setOf("com.b"), viewModel.selectedApps.toSet(), "failed apps stay selected")
        assertEquals(
            mapOf("com.a" to true, "com.b" to false, "com.c" to false),
            viewModel.apps.associate { it.packageName to it.isUninstalled },
        )
    }

    @Test
    fun badgesFromTheBloatListEndUpOnTheirApps() = runVmTest {
        val bloatList = """{"com.a": {"description": "Ads", "removal": "Recommended"}}"""
        val viewModel = AppListViewModel(handler, offlineBloatRepository(bloatList))

        viewModel.loadApps().join()

        assertEquals(
            mapOf("com.a" to AppBadgeInfo.RECOMMENDED, "com.b" to null, "com.c" to null),
            viewModel.apps.associate { it.packageName to it.badgeInfo },
        )
        assertEquals("Ads", viewModel.apps.first { it.packageName == "com.a" }.description)
    }

    @Test
    fun reloadingReusesIconsWithoutStreamingThemAgain() = runVmTest {
        handler.icons = flowOf("com.a" to icon, "com.b" to icon, "com.c" to icon)
        val viewModel = viewModel()
        viewModel.loadApps().join()

        viewModel.loadApps().join()

        assertEquals(1, handler.iconLoads)
        assertEquals(mapOf("com.a" to true, "com.b" to true, "com.c" to true), viewModel.hasIcon())
    }

    @Test
    fun iconsStreamedAfterTheListIsFilledAreApplied() = runVmTest {
        val icons = Channel<Pair<String, ImageBitmap>>(Channel.UNLIMITED)
        handler.icons = icons.consumeAsFlow()
        val viewModel = viewModel()

        val load = viewModel.loadApps()
        assertEquals(mapOf("com.a" to false, "com.b" to false, "com.c" to false), viewModel.hasIcon(), "list is shown before icons")

        icons.send("com.b" to icon)
        icons.close()
        load.join()

        assertEquals(mapOf("com.a" to false, "com.b" to true, "com.c" to false), viewModel.hasIcon())
    }
}
