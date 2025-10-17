// ui/components/BookCard.kt
package com.example.pocketlibrary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pocketlibrary.data.local.entity.BookEntity
import com.example.pocketlibrary.data.remote.model.BookDoc

/**
 * This file contains small, reusable UI "cards" that render book information.
 * We have:
 * - SearchBookCard  : used for Open Library search results (BookDoc, not saved yet)
 * - LibraryBookCard : used for items saved locally (BookEntity from Room)
 * - BookCoverImage  : a shared image box with placeholder + personal-photo badge
 *
 * Why separate components?
 * - Keeps screens clean (List screens just assemble these)
 * - Encourages consistent look & feel
 * - Easier to test/preview each piece
 */

/* ──────────────────────────────────────────────────────────────────────────────
   SearchBookCard
   Renders a search result row with cover, title, author, year, and an Add button.
   Receives a BookDoc (API model), not a Room entity.
   onAddClick  : called when user taps "Add"
   onClick     : called when user taps the card (e.g., open details)
   isInFavorites : disables the Add button and shows a "Check" icon when true
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun SearchBookCard(
    book: BookDoc,
    isInFavorites: Boolean,
    onAddClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        // We let the card fill width and add outer padding for spacing between rows.
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // Entire card is tappable to open details.
            .clickable { onClick() },
        // Small elevation to lift card off background slightly.
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        // Use surface color so the card follows the theme (light/dark).
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        // Rounded corners for a softer look.
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: book cover (or placeholder). We use "M" size for list rows to keep it light.
            BookCoverImage(
                coverUrl = book.getCoverImageUrl("M"),
                title = book.title,
                modifier = Modifier.size(width = 80.dp, height = 120.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Right: textual info + action
            Column(
                modifier = Modifier.weight(1f) // Take all remaining horizontal space
            ) {
                // Title: bold, up to 2 lines with ellipsis to avoid overflow.
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Author row with a small leading icon.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Author",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = book.getAuthorsString(), // Joins multiple authors or "Unknown Author"
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Year: only show if available to avoid empty space.
                book.firstPublishYear?.let { year ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Year",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add button: disabled if already in favorites.
                Button(
                    onClick = onAddClick,
                    enabled = !isInFavorites,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInFavorites)
                            MaterialTheme.colorScheme.surfaceVariant // muted color when disabled
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isInFavorites) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isInFavorites) "In Library" else "Add",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   LibraryBookCard
   Renders a saved book from Room (BookEntity) with actions:
   - Photo: attach or update a personal photo
   - Delete: remove from library (with confirmation dialog)
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun LibraryBookCard(
    book: BookEntity,
    onDeleteClick: () -> Unit,
    onCameraClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local UI state: whether the delete confirmation dialog is visible.
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prefer user's personal photo when present, otherwise fallback to API cover.
            BookCoverImage(
                coverUrl = book.personalPhotoPath ?: book.coverUrl,
                title = book.title,
                modifier = Modifier.size(width = 80.dp, height = 120.dp),
                isPersonalPhoto = book.personalPhotoPath != null // shows a small camera badge
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Author
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Author",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Year if available
                book.year?.let { year ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Year",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Show a small badge if the entry was created manually (not from API).
                if (book.isManualEntry) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Manual Entry",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Manual Entry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row of action buttons: Photo + Delete
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Photo/Update button: opens camera/gallery flow in your screen.
                    OutlinedButton(
                        onClick = onCameraClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Add/Update Photo",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (book.personalPhotoPath != null) "Update" else "Photo",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Delete button: soft-colored (outlined) with error tint for clarity.
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }

    // A simple confirm dialog to prevent accidental deletion.
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false }, // tapping outside or back
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(text = "Delete Book?") },
            text = {
                Text(
                    text = "Are you sure you want to remove '${book.title}' from your library?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick() // bubble up to the screen ViewModel to actually delete
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
   BookCoverImage
   Shared image box:
   - If coverUrl is present → load it with Coil's AsyncImage.
   - If not → show a default icon + "No Cover" label.
   - If it's a personal photo → show a tiny camera badge at the top-right.
   ────────────────────────────────────────────────────────────────────────────── */
@Composable
fun BookCoverImage(
    coverUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    isPersonalPhoto: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)) // rounded corners for the image box
            .background(MaterialTheme.colorScheme.surfaceVariant), // subtle bg for placeholder
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            // AsyncImage automatically handles loading/caching. You can add placeholder/error if you want.
            AsyncImage(
                model = coverUrl,
                contentDescription = "Cover of $title", // accessibility text
                modifier = Modifier.fillMaxSize(),       // occupy the full box
                contentScale = ContentScale.Crop         // crop to avoid letterboxing
            )

            // If it's a personal photo, show a tiny badge overlay in the corner.
            if (isPersonalPhoto) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Personal Photo",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        } else {
            // Fallback when there's no cover URL: show a simple, centered placeholder.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "No cover",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No Cover",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
