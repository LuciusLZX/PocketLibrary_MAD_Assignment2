// ui/screens/LibraryScreen.kt
package com.example.pocketlibrary.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketlibrary.data.local.entity.BookEntity
import com.example.pocketlibrary.ui.components.*
import com.example.pocketlibrary.util.CameraUtils
import com.example.pocketlibrary.viewmodel.LibraryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

/**
 * Library Screen — shows the user’s saved (favorite) books.
 *
 * this screen:
 * - Displays the list of favorites from Room
 * - Lets the user search within saved books (offline filter).
 * - Lets the user delete an item (local, plus tries cloud).
 * - Lets the user attach a personal photo via camera (permission + file uri).
 * - Lets the user share a book to another app/contact.
 * - Can sync with Firebase (menu actions).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    // ──────────────────────────
    // Collect ViewModel state
    // Using collectAsState() subscribes to Flow and recomposes on changes.
    // ──────────────────────────
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()

    // ──────────────────────────
    // Camera state
    // We keep which book is getting a photo, and the file path for the captured image.
    // ──────────────────────────
    var selectedBookForCamera by remember { mutableStateOf<BookEntity?>(null) }
    var currentPhotoPath by remember { mutableStateOf<String?>(null) }

    // Permission state for CAMERA
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Camera launcher using the Activity Result API.
    // TakePicture() expects a content Uri to write the photo into.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // This callback is invoked after camera returns.
        if (success && currentPhotoPath != null && selectedBookForCamera != null) {
            // Persist the photo path for this book in Room (and sync later).
            viewModel.addPersonalPhoto(
                bookId = selectedBookForCamera!!.id,
                photoPath = currentPhotoPath!!
            )
            // Clear the selection after we’re done.
            selectedBookForCamera = null
        }
    }

    // ──────────────────────────
    // Contacts (share) state
    // ──────────────────────────
    var selectedBookForShare by remember { mutableStateOf<BookEntity?>(null) }

    // Permission to read contacts for the contact picker (optional depending on your target/flow)
    val contactsPermission = rememberPermissionState(Manifest.permission.READ_CONTACTS)

    // Contact picker launcher — returns a contact Uri or null.
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri ->
        // If user picked a contact, share the book via chooser.
        contactUri?.let { uri ->
            selectedBookForShare?.let { book ->
                shareBookWithContact(context, uri, book)
                selectedBookForShare = null
            }
        }
    }

    // ──────────────────────────
    // Snackbar host (transient messages)
    // show success / error toast-like messages here.
    // ──────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success message when it changes
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSuccess()
        }
    }
    // Show error message when it changes from null -> value.
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    // ──────────────────────────
    // “More” menu state (top-right)
    // ──────────────────────────
    var showMenu by remember { mutableStateOf(false) }

    // ──────────────────────────
    // Screen layout shell (Scaffold gives us top bar + snackbar slot)
    // ──────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // If syncing, show a small spinner in the top bar (non-blocking indicator)
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    // “More” menu icon button
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }

                    // Dropdown menu for cloud actions
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // Push all unsynced local changes up to Firebase
                        DropdownMenuItem(
                            text = { Text("Sync to Cloud") },
                            onClick = {
                                showMenu = false
                                viewModel.syncToFirebase()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                            }
                        )
                        // Pull remote items from Firebase and merge locally
                        DropdownMenuItem(
                            text = { Text("Refresh from Cloud") },
                            onClick = {
                                showMenu = false
                                viewModel.syncFromFirebase()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                            }
                        )

                        // Add a visual separator
                        Divider()

                        // Logout the user from their Firebase account
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                showMenu = false // Close the menu first
                                onLogout()       // Then trigger the logout action
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Logout"
                                )
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier
    ) { paddingValues ->
        // Main content area under the app bar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ───────── Search + status area ─────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // If offline, show a banner so the user understands results are local-only
                if (!isNetworkAvailable) {
                    MessageBanner(
                        message = "Offline mode - showing locally saved books",
                        type = MessageType.INFO,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Local search (filters favorites live)
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { /* filtering already happens as you type */ },
                    placeholder = "Search your library...",
                    isLoading = false
                )
            }

            Divider()

            // ───────── Body: list / empty-states / loading ─────────
            when {
                isLoading -> {
                    LoadingScreen(message = "Loading your library...")
                }

                // No favorites saved and no search text → show friendly empty state
                favoriteBooks.isEmpty() && searchQuery.isEmpty() -> {
                    EmptyLibrary(onSearchBooks = onNavigateToSearch)
                }

                // There is a search query but no filtered results
                favoriteBooks.isEmpty() && searchQuery.isNotEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Default.SearchOff,
                        title = "No books found",
                        description = "No books match \"$searchQuery\" in your library.",
                        actionText = "Clear Search",
                        onActionClick = { viewModel.clearSearch() }
                    )
                }

                // Normal case: we have books to show
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header row: counter + clear-search (if active)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${favoriteBooks.size} book(s)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (searchQuery.isNotEmpty()) {
                                    TextButton(onClick = { viewModel.clearSearch() }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear")
                                    }
                                }
                            }
                        }

                        // One card per BookEntity
                        items(
                            items = favoriteBooks,
                            key = { it.id } // stable key improves list performance
                        ) { book ->
                            LibraryBookCard(
                                book = book,
                                onDeleteClick = {

                                    viewModel.deleteBook(book)
                                },
                                onCameraClick = {
                                    // Handle camera permission → if granted, open camera
                                    if (cameraPermission.status.isGranted) {
                                        launchCamera(
                                            context = context,
                                            cameraLauncher = cameraLauncher,
                                            onPhotoPathCreated = { path ->
                                                currentPhotoPath = path
                                                selectedBookForCamera = book
                                            }
                                        )
                                    } else {
                                        cameraPermission.launchPermissionRequest()
                                    }
                                },
                                onClick = {
                                    // Use card click to share: pick a contact then share text
                                    selectedBookForShare = book
                                    if (contactsPermission.status.isGranted) {
                                        contactPickerLauncher.launch(null)
                                    } else {
                                        contactsPermission.launchPermissionRequest()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Launch the camera app to capture a photo into a file.
 * 1) create a temp file (CameraUtils.createImageFile),
 * 2) get a FileProvider Uri for it (CameraUtils.getUriForFile),
 * 3) remember the absolute path so we can save it later,
 * 4) launch the camera with that Uri so the camera writes into our file.
 */
private fun launchCamera(
    context: android.content.Context,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    onPhotoPathCreated: (String) -> Unit
) {
    try {
        val photoFile = CameraUtils.createImageFile(context)
        val photoUri = CameraUtils.getUriForFile(context, photoFile)

        onPhotoPathCreated(photoFile.absolutePath) // keep the absolute path for Room

        cameraLauncher.launch(photoUri) // open the camera app writing into that Uri
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Share a book with a selected contact using ACTION_SEND.
 * We read the picked contact then compose a share text and open the chooser.
 */
private fun shareBookWithContact(
    context: android.content.Context,
    contactUri: Uri,
    book: BookEntity
) {
    try {
        // Query the contact just to display their name in the chooser title
        val cursor = context.contentResolver.query(
            contactUri,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val contactName = if (nameIndex >= 0) it.getString(nameIndex) else "Contact"

                // Build the share message
                val shareText = buildString {
                    append("📚 Book Recommendation\n\n")
                    append("Title: ${book.title}\n")
                    append("Author: ${book.author}\n")
                    book.year?.let { year -> append("Year: $year\n") }
                    append("\nShared via Pocket Library")
                }

                // Send the text to any compatible app (SMS, WhatsApp, Email)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "Book Recommendation: ${book.title}")
                }

                context.startActivity(
                    Intent.createChooser(shareIntent, "Share book with $contactName")
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

