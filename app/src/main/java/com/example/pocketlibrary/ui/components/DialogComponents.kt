// ui/components/DialogComponents.kt
package com.example.pocketlibrary.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * This file defines small, reusable dialog components.
 */

/* ──────────────────────────────────────────────────────────────────────────────
   ConfirmationDialog
   A generic “Are you sure?” style dialog with optional icon and destructive style.
   Use it for anything that needs a confirm + cancel flow.

   Parameters:
   - title, message: text content for the dialog.
   - confirmText, dismissText: button labels (“Confirm”, “Cancel").
   - onConfirm: called when user taps the confirm button.
   - onDismiss: called when user taps cancel, outside, or back (via onDismissRequest).
   - isDestructive: if true, styles confirm button with error color (red).
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm", // default label for confirm action
    dismissText: String = "Cancel",  // default label for cancel action
    onConfirm: () -> Unit,           // callback when user confirms
    onDismiss: () -> Unit,           // callback when user cancels/dismisses
    icon: ImageVector? = null,       // optional leading icon
    isDestructive: Boolean = false   // if true → red confirm button + red icon tint
) {
    AlertDialog(
        onDismissRequest = onDismiss, // called when user taps outside/back
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isDestructive)
                        MaterialTheme.colorScheme.error   // red for destructive actions
                    else
                        MaterialTheme.colorScheme.primary // primary color for normal actions
                )
            }
        },
        title = {
            Text(text = title)
        },
        // Main message body
        text = {
            Text(text = message)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                // If destructive, paint the button red to warn the user
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors() // use default themed colors
                }
            ) {
                Text(confirmText)
            }
        },
        // Dismiss button on the left
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/* ──────────────────────────────────────────────────────────────────────────────
   DeleteConfirmationDialog
   A convenience wrapper around ConfirmationDialog pre-configured for deletes.
   ───────────────────────────────────────────────────────────────────────────── */
@Composable
fun DeleteConfirmationDialog(
    itemName: String,        // name of what we are deleting
    onConfirm: () -> Unit,   // called when user confirms deletion
    onDismiss: () -> Unit    // called when user cancels
) {
    ConfirmationDialog(
        title = "Delete $itemName?",
        message = "This action cannot be undone.",
        confirmText = "Delete",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        icon = Icons.Default.Delete,
        isDestructive = true         // style confirm button as destructive (red)
    )
}

/* ──────────────────────────────────────────────────────────────────────────────
   InfoDialog
   A simple “OK-only” dialog for informational messages.
   Use this when just want to tell the user something and let them dismiss.
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun InfoDialog(
    title: String,          // short heading
    message: String,        // body copy
    onDismiss: () -> Unit   // called when user taps OK or outside/back
) {
    AlertDialog(
        onDismissRequest = onDismiss, // allow tapping outside/back to close
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null
            )
        },
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        // Only a single OK button
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
