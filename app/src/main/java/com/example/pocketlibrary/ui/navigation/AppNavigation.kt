package com.example.pocketlibrary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pocketlibrary.data.repository.AuthRepository
import com.example.pocketlibrary.ui.screens.LibraryScreen
import com.example.pocketlibrary.ui.screens.ManualEntryScreen
import com.example.pocketlibrary.ui.screens.SearchScreen
import com.example.pocketlibrary.ui.screens.LoginScreen
import com.example.pocketlibrary.viewmodel.LibraryViewModel
import com.example.pocketlibrary.viewmodel.ManualEntryViewModel
import com.example.pocketlibrary.viewmodel.SearchViewModel
import com.example.pocketlibrary.viewmodel.ViewModelFactory

/**
 * We define our app's "routes" (screen IDs) in a sealed class.
 * Why: avoids hard-coded strings everywhere and gives us type safety-ish ergonomics.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")             // Route string for Login screen
    object Search : Screen("search")           // Route string used to navigate to Search screen
    object Library : Screen("library")         // Route string for Library screen
    object ManualEntry : Screen("manual_entry")// Route string for Manual Entry screen
}

/**
 * AppNavigation wires up the NavHost (the container that swaps screens)
 * and maps each route to a Composable screen.
 *
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Search.route, // default to Search screen on launch
    modifier: Modifier = Modifier,
    viewModelFactory: ViewModelProvider.Factory,
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit, // Callback for successful login
    onLogout: () -> Unit,
) {
    // NavHost is the "stage" where different screens (destinations) are displayed.
    // - navController drives navigation actions (navigate, popBackStack, etc.)
    // - startDestination tells which route shows first.
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ─────────────────────────────
        // Login Screen destination
        // ─────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                authRepository = authRepository,
                onLoginSuccess = onLoginSuccess // Allow redirect to the main screen
            )
        }

        // ─────────────────────────────
        // Search Screen destination
        // ─────────────────────────────
        composable(Screen.Search.route) {

            // Create the ViewModel using the factory
            val searchViewModel: SearchViewModel = viewModel(factory = viewModelFactory)

            // We pass lambdas into the screen so it can request navigation without
            // directly depending on NavController
            SearchScreen(
                viewModel = searchViewModel,
                onNavigateToManualEntry = {
                    // Push ManualEntry onto the back stack.
                    // After this, pressing back will return to Search.
                    navController.navigate(Screen.ManualEntry.route)
                }
            )
        }

        // ─────────────────────────────
        // Library Screen destination
        // ─────────────────────────────
        composable(Screen.Library.route) {

            // Create the ViewModel using the factory
            val libraryViewModel: LibraryViewModel = viewModel(factory = viewModelFactory)
            LibraryScreen(
                viewModel = libraryViewModel,
                onNavigateToSearch = {
                    // Navigate to Search. The popUpTo clears parts of the back stack.
                    navController.navigate(Screen.Search.route) {
                        popUpTo(Screen.Search.route) { inclusive = false }
                    }
                },
                onLogout = onLogout // Pass the logout callback
            )
        }

        // ─────────────────────────────
        // Manual Entry Screen destination
        // ─────────────────────────────
        composable(Screen.ManualEntry.route) {

            // Create the ViewModel using the factory
            val manualEntryViewModel: ManualEntryViewModel = viewModel(factory = viewModelFactory)
            ManualEntryScreen(
                viewModel = manualEntryViewModel, // Pass the ViewModel to the screen
                onNavigateBack = {
                    // Pop the topmost destination (ManualEntry) and return to the previous one.
                    navController.popBackStack()
                }
            )
        }
    }
}
