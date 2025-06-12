package io.github.samolego.canta.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Logs : Screen("logs")
    data object Settings : Screen("settings")
}
