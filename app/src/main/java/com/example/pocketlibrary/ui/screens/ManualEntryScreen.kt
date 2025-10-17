// ui/screens/ManualEntryScreen.kt
package com.example.pocketlibrary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketlibrary.viewmodel.ManualEntryViewModel

/**
 * Manual Entry Screen — lets the user add a book by hand (no API).
 *
 *  use this:
 * - No internet
 * - Book not found in Open Library
 * - User prefers to type it in
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    viewModel: ManualEntryViewModel,
    onNavigateBack: () -> Unit,                // Callback to return to previous screen
    modifier: Modifier = Modifier
) {
    // ─────────────────────────────────────────
    // Collect state from the ViewModel
    // Each collectAsState causes recomposition when data changes.
    // ─────────────────────────────────────────
    val title by viewModel.title.collectAsState()                 // Current title text
    val author by viewModel.author.collectAsState()               // Current author text
    val year by viewModel.year.collectAsState()                   // Current year text (string)

    val titleError by viewModel.titleError.collectAsState()       // Error message for title or null
    val authorError by viewModel.authorError.collectAsState()     // Error message for author or null
    val yearError by viewModel.yearError.collectAsState()         // Error message for year or null

    val isLoading by viewModel.isLoading.collectAsState()         // True while saving
    val isSuccess by viewModel.isSuccess.collectAsState()         // True when save succeeded
    val errorMessage by viewModel.errorMessage.collectAsState()   // One-off error message for snackbar
    val successMessage by viewModel.successMessage.collectAsState()// Optional success message

    // ─────────────────────────────────────────
    // Snackbar host
    // ─────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    // If save succeeded → navigate back and reset success flag.
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            onNavigateBack()
            viewModel.resetSuccess() // So we don’t trigger again on recomposition
        }
    }

    // If errorMessage changes (non-null), show it via snackbar then clear it.
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Book Manually") }, // Screen title
                navigationIcon = {
                    // Back arrow — calls the provided callback
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─────────────────────────────────
            // Info card
            // ─────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "📖 Add Book Offline",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Enter book details manually. This is useful when you're offline or the book isn't found in our database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // ─────────────────────────────────
            // Form fields
            // ─────────────────────────────────

            // Title (required)
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Book Title *") },
                placeholder = { Text("e.g., Pride and Prejudice") },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Author (required)
            OutlinedTextField(
                value = author,
                onValueChange = { viewModel.updateAuthor(it) },
                label = { Text("Author *") },
                placeholder = { Text("e.g., Jane Austen") },
                isError = authorError != null,
                supportingText = authorError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Year (numeric only)
            OutlinedTextField(
                value = year,
                onValueChange = { viewModel.updateYear(it) },
                label = { Text("Publication Year (Optional)") },
                placeholder = { Text("e.g., 1813") },
                isError = yearError != null,
                supportingText = yearError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number // Pops up numeric keypad
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Little note about required fields
            Text(
                text = "* Required fields",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // ─────────────────────────────────
            // Action buttons: Cancel / Add Book
            // ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cancel: just go back (disabled while loading)
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }

                // Add Book: triggers VM.addBook() which validates then saves
                Button(
                    onClick = { viewModel.addBook() },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        // Small inline spinner while saving
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add Book")
                    }
                }
            }

            // ─────────────────────────────────
            // Helpful hints
            // ─────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Tips",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Enter the title exactly as it appears on the book\n" +
                                "• Use full author names (e.g., Jane Austen, not J. Austen)\n" +
                                "• Year is optional but helps organize your library\n" +
                                "• You can add a personal photo later using the camera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

