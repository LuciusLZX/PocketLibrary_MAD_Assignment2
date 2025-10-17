// ui/screens/SearchScreen.kt
package com.example.pocketlibrary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketlibrary.ui.components.*
import com.example.pocketlibrary.viewmodel.SearchViewModel

/**
 * Search Screen — this is the main page where the user searches books online.
 *
 * it shows:
 * - A search bar at the top.
 * - A floating button to add a book manually (when API is not enough or offline).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,  // Obtain a VM scoped to this screen
    onNavigateToManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ─────────────────────────────
    // Collect state from ViewModel
    // ─────────────────────────────

    // The text typed in the search box.
    val searchQuery by viewModel.searchQuery.collectAsState()

    // The list of results returned by the Open Library API.
    val searchResults by viewModel.searchResults.collectAsState()

    // True while we are calling the API (so we can show a spinner).
    val isLoading by viewModel.isLoading.collectAsState()

    // One-off error text to show in a snackbar (e.g., "Network error").
    val errorMessage by viewModel.errorMessage.collectAsState()

    // One-off success text to show in a snackbar (e.g., "Added to favorites").
    val successMessage by viewModel.successMessage.collectAsState()

    // Whether the device appears to be online (so we can guide the user).
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()

    // IDs of books that are already in favorites (to disable the Add button in results).
    val favoriteBookIds by viewModel.favoriteBookIds.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // When successMessage changes
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearSuccess()
        }
    }

    // When errorMessage changes
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    // ─────────────────────────────
    // Scaffold
    // ─────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Books") }, // The app bar title
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            // Floating Action Button to jump to "manual entry" screen
            ExtendedFloatingActionButton(
                onClick = onNavigateToManualEntry,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Manually") },
                text = { Text("Add Manually") },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        },
        snackbarHost = {

            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier
    ) { paddingValues ->
        // Main column that holds search bar and results.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ───────── Search Bar Section ─────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // If we detect no internet, show a warning banner to guide the user.
                if (!isNetworkAvailable) {
                    MessageBanner(
                        message = "No internet connection. Use manual entry to add books.",
                        type = MessageType.WARNING,
                        onDismiss = null,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Search input + Search button.
                SearchBarWithButton(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = {
                        viewModel.checkNetworkAvailability()
                        viewModel.searchBooks()
                    },
                    placeholder = "Search by title or author...",
                    isLoading = isLoading
                )
            }

            Divider()

            // ───────── Results Section ─────────
            when {
                // If we are loading, we cover the body with a full-screen spinner and a message.
                isLoading -> {
                    LoadingScreen(message = "Searching books...")
                }

                // If offline and we have no results to show, offer a friendly offline state with a Retry.
                !isNetworkAvailable && searchResults.isEmpty() -> {
                    NoInternetConnection(
                        onRetry = {
                            viewModel.checkNetworkAvailability()
                            if (searchQuery.isNotEmpty()) {
                                viewModel.searchBooks()
                            }
                        }
                    )
                }

                // If a search was performed (query is not empty) but no matches came back, show an empty state.
                searchResults.isEmpty() && searchQuery.isNotEmpty() -> {
                    EmptySearchResults(
                        query = searchQuery,
                        onManualEntry = onNavigateToManualEntry
                    )
                }

                // If we haven't searched yet (query empty) and there are no results, show a neutral prompt.
                searchResults.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "Search for books",
                        description = "Enter a book title or author name to start searching the Open Library catalog."
                    )
                }

                // Normal case: we have results to display.
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp) // Leave room for the FAB
                    ) {
                        // A simple header that shows how many items were found.
                        item {
                            Text(
                                text = "Found ${searchResults.size} books",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        // Render one card per book result.
                        items(
                            items = searchResults,
                            key = { it.key } // Stable key improves list performance and animations
                        ) { book ->
                            SearchBookCard(
                                book = book,
                                isInFavorites = favoriteBookIds.contains(book.key), // Disable "Add" if already saved
                                onAddClick = {
                                    viewModel.addBookToFavorites(book) // Save to Room and try to sync
                                },
                                onClick = {
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

