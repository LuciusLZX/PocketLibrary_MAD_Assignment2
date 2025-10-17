// ui/components/LoadingIndicator.kt
package com.example.pocketlibrary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * This file defines small, reusable loading UI pieces:
 * - LoadingScreen   : full-screen loader for entire screens (blocking)
 * - InlineLoadingIndicator : small spinner inside a list/section (non-blocking)
 * - LoadingOverlay  : semi-transparent overlay on top of current content
 */

/* ──────────────────────────────────────────────────────────────────────────────
   LoadingScreen
   Use this when an entire screen is in a loading state
   Fills the whole screen and centers a spinner + message.
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun LoadingScreen(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    // Box takes all space so we can center content both vertically and horizontally.
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // The main spinner
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(16.dp)) // Space between spinner and text

            // loading message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   InlineLoadingIndicator
   Use this inside sections ( below a list while paging, or replacing a card).
   Does not block the whole screen; just shows a small centered spinner line.
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun InlineLoadingIndicator(
    modifier: Modifier = Modifier
) {
    // Full width so the spinner appears centered relative to the section width.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Smaller spinner for inline use
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   LoadingOverlay
   A semi-transparent overlay with a spinner + message on top of existing content.
   Great for short, blocking tasks (submitting a form, syncing).
   Shows a card in the middle to focus attention.
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun LoadingOverlay(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    // Surface covers the whole screen with a translucent color,
    // dimming the background content without removing it.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) // Dim the background
    ) {
        // Center the “dialog-like” card
        Box(contentAlignment = Alignment.Center) {
            // Elevated card to stand out from the dimmed background
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Message text
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
