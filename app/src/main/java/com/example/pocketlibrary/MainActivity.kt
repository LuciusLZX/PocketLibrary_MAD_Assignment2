package com.example.pocketlibrary

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pocketlibrary.data.repository.AuthRepository
import com.example.pocketlibrary.data.repository.BookRepository
import com.example.pocketlibrary.ui.navigation.AppNavigation
import com.example.pocketlibrary.ui.navigation.Screen
import com.example.pocketlibrary.ui.theme.PocketLibraryTheme
import com.example.pocketlibrary.util.FirebaseTest
import com.example.pocketlibrary.viewmodel.ViewModelFactory
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import kotlin.getValue

class MainActivity : ComponentActivity() {

    // --- Create the single source of truth for the repositories ---
    private val authRepository by lazy { AuthRepository() }
    private val bookRepository by lazy { BookRepository(this) }

    // --- Create the ViewModelFactory using the repositories ---
    private val viewModelFactory by lazy {
        ViewModelFactory(
            bookRepository,
            authRepository
        ) // All ViewModel will use the same repositories instance
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Firebase initialization ---
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}", e)
        }

        // --- Quick smoke test for Firebase (Auth/Firestore).For testing only, can be removed. ---
        lifecycleScope.launch {
            try {
                FirebaseTest.runAllTests() // Anonymous sign-in + Firestore write/read/delete check.
            } catch (e: Exception) {
                Log.e(TAG, "Firebase test failed: ${e.message}", e)
            }
        }
        setContent {
            PocketLibraryTheme {
                PocketLibraryApp(
                    viewModelFactory = viewModelFactory,
                    authRepository = authRepository,
                    bookRepository = bookRepository
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketLibraryApp(
    viewModelFactory: ViewModelFactory,
    authRepository: AuthRepository,
    bookRepository: BookRepository
) {
    val navController = rememberNavController() // Create and remember a NavController for navigation.

    // Observe current destination as Compose state so the bottom bar updates automatically on navigation.
    val navBackStackEntry by navController.currentBackStackEntryAsState() // Recompose when the destination changes.
    val currentDestination = navBackStackEntry?.destination // Can be null on very first composition.

    // Check if a user properly logged in to decide the redirected screen
    val startDestination = if (authRepository.currentUser != null) {
        Screen.Search.route
    } else {
        Screen.Login.route
    }

    // Decide if the bottom bar should be visible on the current screen.
    val showBottomBar = currentDestination?.route in listOf( // Only show on Search and Library screens.
        Screen.Search.route,
        Screen.Library.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) { // Hide the bar on other screens (Manual Entry).
                NavigationBar {
                    bottomNavItems.forEach { item -> // Render each tab item from a list.
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.route == item.route // Check if the current destination belongs to this tab’s hierarchy.
                        } == true

                        NavigationBarItem(
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) }, // Tab icon.
                            label = { Text(item.label) }, // Tab label under the icon.
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(item.route) { // Navigate to the tab’s root route.
                                        popUpTo(Screen.Search.route) { // Pop back stack up to the start destination.
                                            saveState = true // Keep state (scroll position) of popped destinations.
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues -> // Insets provided by Scaffold (e.g., space for bottom bar).
        AppNavigation( // Your NavHost with composable(Screen.Search/Library/ManualEntry) destinations.
            navController = navController, // The same controller we created above.
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues), // Prevent content from drawing under the bottom bar.
            viewModelFactory = viewModelFactory,
            authRepository = authRepository, // <-- ADD THIS LINE
            // Pass back the lambda of the login
            onLoginSuccess = {
                // After login, navigate to the search screen and clear the login screen from history
                navController.navigate(Screen.Search.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            },

            // Pass back the lambda of the logout
            onLogout = {
                authRepository.logout()
                // After logout, navigate to the login screen and clear the entire app history
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        )
    }
}


/**
 * Bottom navigation item data class
 */
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
)

/**
 * Bottom navigation items list
 */
private val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Search.route,
        icon = Icons.Default.Search,
        label = "Search"
    ),
    BottomNavItem(
        route = Screen.Library.route,
        icon = Icons.Default.LibraryBooks,
        label = "My Library"
    )
)