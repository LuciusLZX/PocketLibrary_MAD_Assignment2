package com.example.pocketlibrary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Message display helpers:
 * - MessageSnackbar
 * - MessageBanner
 * - MessageHandler
 */

/* ──────────────────────────────────────────────────────────────────────────────
   Types & icon/color helpers
   ────────────────────────────────────────────────────────────────────────────── */

enum class MessageType { SUCCESS, ERROR, WARNING, INFO }

/** Pick an icon that visually matches the message type. */
private fun getMessageIcon(type: MessageType): ImageVector = when (type) {
    MessageType.SUCCESS -> Icons.Default.CheckCircle
    MessageType.ERROR   -> Icons.Default.Error
    MessageType.WARNING -> Icons.Default.Warning
    MessageType.INFO    -> Icons.Default.Info
}

/**
 * Pick a base color for each type.
 */
@Composable
private fun getMessageColor(type: MessageType): Color = when (type) {
    MessageType.SUCCESS -> Color(0xFF4CAF50)
    MessageType.ERROR   -> MaterialTheme.colorScheme.error
    MessageType.WARNING -> Color(0xFFFF9800)
    MessageType.INFO    -> MaterialTheme.colorScheme.primary
}

/* ──────────────────────────────────────────────────────────────────────────────
   MessageSnackbar
   A self-contained snackbar
   Place inside a Box/Scaffold, or anywhere a Snackbar can render.
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun MessageSnackbar(
    message: String,
    type: MessageType = MessageType.INFO,
    onDismiss: () -> Unit,
    autoHide: Long = 3000,            // 0 or negative disables auto-hide
    modifier: Modifier = Modifier
) {
    val icon = getMessageIcon(type)
    val color = getMessageColor(type)

    // Auto-hide after the given duration (restarts when message text changes)
    LaunchedEffect(message) {
        if (autoHide > 0) {
            delay(autoHide)
            onDismiss()
        }
    }

    Snackbar(
        modifier = modifier.padding(16.dp),
        containerColor = color,
        contentColor = Color.White,
        action = {
            TextButton(onClick = onDismiss) {
                Text(text = "Dismiss", color = Color.White)
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(text = message)
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   MessageBanner
   An inline banner that can drop at the top of a screen or between content.
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun MessageBanner(
    message: String,
    type: MessageType = MessageType.INFO,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val icon = getMessageIcon(type)
    val color = getMessageColor(type)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = message,
                modifier = Modifier.weight(1f), // text takes remaining space
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = color
                    )
                }
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   MessageHandler
   A small helper to show success/error messages using a single SnackbarHost.
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun MessageHandler(
    successMessage: String?,
    errorMessage: String?,
    onDismissSuccess: () -> Unit,
    onDismissError: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    // Show success once when the string changes
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short // success usually shorter
            )
            onDismissSuccess()
        }
    }

    // Show error once when the string changes from null → something
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long // errors usually longer
            )
            onDismissError()
        }
    }

    // The host that actually renders the snackbars produced above
    SnackbarHost(hostState = snackbarHostState)
}
