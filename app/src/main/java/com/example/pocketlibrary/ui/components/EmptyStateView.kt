package com.example.pocketlibrary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * EmptyStateView is used at any time a screen has no content yet (no data, error, offline).
 */

/**
 * Generic Empty State view.
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // A vertical stack that fills the whole screen so we can truly center contents.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            // Slightly transparent
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp)) // Space between icon and title

        // Headline
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        // Optional supporting text: only render when provided.
        description?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Optional primary action button.
        // We show it only if BOTH label and click handler exist.
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onActionClick) {
                Text(text = actionText)
            }
        }
    }
}

/* =============================================================================
   Preconfigured variants
   These wrap EmptyStateView with the right icon/text.
   ========================================================================== */

/**
 * Empty state for "no search results".
 */
@Composable
fun EmptySearchResults(
    query: String,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.SearchOff,
        title = "No books found",
        description = "No results for \"$query\". Try a different search or add manually.",
        actionText = "Add Manually",
        onActionClick = onManualEntry,
        modifier = modifier
    )
}

/**
 * Empty state for the library screen when user has no favorites yet.
 */
@Composable
fun EmptyLibrary(
    onSearchBooks: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.LibraryBooks, // Visual: books/library
        title = "Your library is empty",
        description = "Start building your collection by searching for books or adding them manually.",
        actionText = "Search Books",
        onActionClick = onSearchBooks,
        modifier = modifier
    )
}

/**
 * Offline state when network is unavailable.
 */
@Composable
fun NoInternetConnection(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.WifiOff,
        title = "No internet connection",
        description = "Please check your connection and try again. You can still browse your saved books.",
        actionText = "Retry",
        onActionClick = onRetry,
        modifier = modifier
    )
}

/**
 * Generic error state for unexpected failures.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.ErrorOutline,
        title = "Something went wrong",
        description = message,
        actionText = if (onRetry != null) "Try Again" else null, // Button only if retry exists
        onActionClick = onRetry,
        modifier = modifier
    )
}
