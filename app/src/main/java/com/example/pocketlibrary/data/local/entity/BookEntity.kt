package com.example.pocketlibrary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


// Table: books
// Represents one saved book in the local DB.
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,              // Unique ID

    val title: String,           // Book title

    val author: String,          // Author(s), comma-separated if multiple

    val year: Int? = null,       // First publication year (nullable)

    val coverUrl: String? = null,     // stores the online image URL for the book cover or null if not available.

    val personalPhotoPath: String? = null, // local device file path to the user’s own photo or null if none.

    val isManualEntry: Boolean = false,    // True if manually added offline

    val dateAdded: Long = System.currentTimeMillis(), // Timestamp, newest first

    val syncedToFirebase: Boolean = false  // Cloud sync flag
)

