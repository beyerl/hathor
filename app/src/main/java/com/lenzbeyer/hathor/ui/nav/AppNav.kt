package com.lenzbeyer.hathor.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lenzbeyer.hathor.ui.screens.DownloadScreen
import com.lenzbeyer.hathor.ui.screens.HomeScreen
import com.lenzbeyer.hathor.ui.screens.HomeViewModel
import com.lenzbeyer.hathor.ui.screens.LibraryDetailScreen
import com.lenzbeyer.hathor.ui.screens.LibraryScreen
import com.lenzbeyer.hathor.ui.screens.SettingsScreen
import com.lenzbeyer.hathor.ui.screens.SettingsViewModel
import com.lenzbeyer.hathor.ui.screens.TagPreviewScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val vm: HomeViewModel = hiltViewModel()
            HomeScreen(
                vm = vm,
                onContinue = {
                    nav.navigate(Routes.TAG_PREVIEW)
                    vm.consumed()
                },
                onLibraryClick = { nav.navigate(Routes.LIBRARY) },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) },
                onRecentClick = { id -> nav.navigate(Routes.libraryDetail(id)) },
            )
        }

        composable(Routes.TAG_PREVIEW) {
            TagPreviewScreen(
                vm = hiltViewModel(),
                onBack = { nav.popBackStack() },
                onStartDownload = { playlistId ->
                    nav.navigate(Routes.download(playlistId)) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }

        composable(
            Routes.DOWNLOAD,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
        ) {
            DownloadScreen(
                vm = hiltViewModel(),
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.LIBRARY) {
            LibraryScreen(
                vm = hiltViewModel(),
                onPlaylistClick = { id -> nav.navigate(Routes.libraryDetail(id)) },
                onBack = { nav.popBackStack() },
                onNew = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }

        composable(
            Routes.LIBRARY_DETAIL,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
        ) {
            LibraryDetailScreen(
                vm = hiltViewModel(),
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = hiltViewModel()
            LaunchedEffect(Unit) { vm.loadYtDlpVersion() }
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
