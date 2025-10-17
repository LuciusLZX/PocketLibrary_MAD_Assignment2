package com.example.pocketlibrary.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.pocketlibrary.data.local.database.AppDatabase
import com.example.pocketlibrary.data.local.entity.BookEntity
import com.example.pocketlibrary.data.remote.RetrofitInstance
import com.example.pocketlibrary.data.remote.model.BookDoc
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

import java.util.UUID // This is used to generate unique IDs for manual books when there is no API ID.

class BookRepository(private val context: Context) {

    // ============ Dependencies ============
    /** Room database DAO for local data operations. */
    private val bookDao = AppDatabase.getDatabase(context).bookDao() // This gets a working DAO from our Room singleton.

    /** Firebase Firestore instance for cloud storage. */
    private val firestore = FirebaseFirestore.getInstance() // This gives us the Firestore client singleton.

    /** Firebase Auth for user identification. */
    private val auth = FirebaseAuth.getInstance() // This gives us the Firebase Auth client to get or create a user id.

    /** Firestore collection reference for books. */
    private val booksCollection = firestore.collection("books") // This points to the top-level “books” collection in Firestore.

    /** Tag for logging. */
    private val TAG = "BookRepository" // This is the tag we use in Logcat so logs are grouped and easy to filter.

    // ============ Network Utility ============

    // This function checks if the device currently has an active network with Wi-Fi, cellular, or ethernet.
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || // This returns true if on Wi-Fi,
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || // or on mobile data,
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) // or on ethernet.
    }

    // ============ API Operations (Search Books Online) ============
    // This function searches the Open Library API for books that match the user’s query.
    suspend fun searchBooksOnline(query: String): Result<List<BookDoc>> {
        return try {
            // Step 1: Check internet connection early so we can fail fast with a friendly message.
            if (!isNetworkAvailable()) {
                Log.w(TAG, "No internet connection available") // This logs a warning that we are offline.
                return Result.failure(Exception("No internet connection. Try manual entry."))
            }

            // Step 2: Validate query so we do not waste a network call with empty input.
            if (query.isBlank()) {
                return Result.failure(Exception("Search query cannot be empty"))
            }

            Log.d(TAG, "Searching online for: $query") // This logs what we are searching for to help debugging.

            // Step 3: Make the API call using our Retrofit singleton and pass the trimmed query and a result limit.
            val response = RetrofitInstance.api.searchBooks(query = query.trim(), limit = 20)

            // Step 4: Handle the HTTP response correctly, checking status and body.
            if (response.isSuccessful) {
                val books = response.body()?.docs ?: emptyList()
                Log.d(TAG, "Found ${books.size} books online") // This logs how many results we received.
                Result.success(books) // This returns a success Result with the list of BookDoc items.
            } else {
                // If HTTP was not successful (like 404 or 500), we build an error message with code and reason.
                val errorMsg = "Search failed: ${response.code()} ${response.message()}" // This shows the status code and message.
                Log.e(TAG, errorMsg) // This logs the error for debugging.
                Result.failure(Exception(errorMsg)) // This returns a failure Result so the UI can show an error.
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error searching online: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============ Local Database Operations (Room) ============
    // This function returns a Flow of all favorite books sorted by newest first so the UI can auto-update.
    fun getAllFavorites(): Flow<List<BookEntity>> {
        Log.d(TAG, "Getting all favorites from local database")
        return bookDao.getAllBooks()
    }

    // This function searches favorites offline by title/author and a blank query returns all books.
    fun searchFavorites(query: String): Flow<List<BookEntity>> {
        Log.d(TAG, "Searching favorites for: $query")
        return if (query.isBlank()) {
            bookDao.getAllBooks()
        } else {
            bookDao.searchBooks(query.trim()) // This returns a Flow filtered by the query (case-insensitive LIKE).
        }
    }

    // This function saves an API book into favorites if it is not already saved.
    suspend fun addBookToFavorites(bookDoc: BookDoc): Result<BookEntity> {
        return try {
            Log.d(TAG, "Adding book to favorites: ${bookDoc.title}") // This logs which book we are trying to add.

            // Check if book already exists locally to avoid duplicates.
            val existingBook = bookDao.getBookById(bookDoc.key) // This looks up by the API key (used as our primary key).
            if (existingBook != null) { // This means the book is already saved.
                Log.w(TAG, "Book already in favorites: ${bookDoc.title}")
                return Result.failure(Exception("Book is already in your library"))
            }

            // Convert the API model to a local Room entity using the helper function inside BookDoc.
            val bookEntity = bookDoc.toBookEntity()

            // Save the new entity to the local database first so the UI updates immediately.
            bookDao.insertBook(bookEntity)
            Log.d(TAG, "Book saved to local database")

            // Try to sync to Firebase.
            syncBookToFirebase(bookEntity)

            Result.success(bookEntity) // This returns the saved entity to the caller.

        } catch (e: Exception) {
            Log.e(TAG, "Error adding book to favorites: ${e.message}", e)
            Result.failure(e) // This returns a failure Result for the UI to handle.
        }
    }

    // This function adds a custom book when there is no internet or when the user wants to enter it manually.
    suspend fun addManualBook( title: String, author: String, year: Int? = null): Result<BookEntity> {
        return try {
            // Validate that mandatory fields are present so we do not save bad data.
            if (title.isBlank() || author.isBlank()) {
                return Result.failure(Exception("Title and author are required"))
            }

            Log.d(TAG, "Adding manual book: $title by $author")

            // Create a new entity with a random UUID for the primary key.
            val bookEntity = BookEntity(
                id = UUID.randomUUID().toString(), // This generates a unique string identifier.
                title = title.trim(),
                author = author.trim(),
                year = year,
                coverUrl = null,
                personalPhotoPath = null,
                isManualEntry = true,
                dateAdded = System.currentTimeMillis(),
                syncedToFirebase = false
            )

            // Save the manual book locally first so it shows up in the UI right away.
            bookDao.insertBook(bookEntity)
            Log.d(TAG, "Manual book saved to local database")

            // Try to sync to Firebase, if offline, this will do nothing and we will sync later.
            syncBookToFirebase(bookEntity)

            Result.success(bookEntity)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding manual book: ${e.message}", e)
            Result.failure(e)
        }
    }

    // This function updates an existing book (for example, after editing or adding a photo).
    suspend fun updateBook(book: BookEntity): Result<Int> {
        return try {
            Log.d(TAG, "Updating book: ${book.title}")

            val rowsUpdated = bookDao.updateBook(book)

            if (rowsUpdated > 0) {
                Log.d(TAG, "Book updated successfully")

                syncBookToFirebase(book)

                Result.success(rowsUpdated)
            } else {
                Result.failure(Exception("Book not found in database"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating book: ${e.message}", e)
            Result.failure(e)
        }
    }

    // This function deletes a book from favorites both locally and in Firestore.
    suspend fun deleteBook(book: BookEntity): Result<Unit> { // Unit means void.
        return try {
            Log.d(TAG, "Deleting book: ${book.title}")

            // Delete from local database first so the UI updates immediately.
            bookDao.deleteBook(book)
            Log.d(TAG, "Book deleted from local database")

            // Get the logged in user's ID to allow deletion on the particular Firebase.
            auth.currentUser?.uid?.let {userId ->
                try {
                    booksCollection
                        .document(userId) //
                        .collection("userBooks")
                        .document(book.id) // This points to the specific book document by id.
                        .delete()
                        .await()
                    Log.d(TAG, "Book deleted from Firebase")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete from Firebase: ${e.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting book: ${e.message}", e)
            Result.failure(e)
        }
    }

    // This function quickly checks if a given id is already saved in favorites.
    suspend fun isBookInFavorites(bookId: String): Boolean {
        return try {
            bookDao.isBookInFavorites(bookId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if book in favorites: ${e.message}", e)
            false
        }
    }

    // This function updates ONLY the personal photo path for a book and then syncs to Firestore.
    suspend fun updatePersonalPhoto(bookId: String, photoPath: String): Result<Unit> {
        return try {
            Log.d(TAG, "Updating personal photo for book: $bookId")

            bookDao.updatePersonalPhoto(bookId, photoPath)

            // After updating locally, we fetch the full entity and push it to Firestore.
            val book = bookDao.getBookById(bookId)
            if (book != null) {
                syncBookToFirebase(book)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating personal photo: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ============ Firebase Cloud Storage Operations ============

    // This private helper pushes one book to Firestore and marks it synced if it succeeds.
    private suspend fun syncBookToFirebase(book: BookEntity) {

        if (!isNetworkAvailable()) {
            Log.d(TAG, "No internet - will sync later")
            return
        }

        // Get the current user's ID. Only if user is logged in.
        val userId = auth.currentUser?.uid
        if (userId == null) {  // It will be synced later by syncUnsyncedBooks() after the user logs in.
            Log.w(TAG, "Cannot sync book to Firebase: No user is logged in.")
            return
        }

        try {
            Log.d(TAG, "Syncing book to Firebase: ${book.title}") // This logs which book we are uploading.

            // We convert the entity to a simple map because Firestore stores documents as key/value pairs.
            val bookMap = hashMapOf(
                "id" to book.id,
                "title" to book.title,
                "author" to book.author,
                "year" to book.year,
                "personalPhotoPath" to book.personalPhotoPath,
                "isManualEntry" to book.isManualEntry,
                "dateAdded" to book.dateAdded,
                "userId" to userId
            )

            // This writes the document to /books/{userId}/userBooks/{bookId} using merge so we do not overwrite other fields.
            booksCollection
                .document(userId)
                .collection("userBooks")
                .document(book.id)
                .set(bookMap, SetOptions.merge())
                .await()

            Log.d(TAG, "Book synced to Firebase successfully")

            // After a successful upload, we mark the local row as synced so we do not re-upload it unnecessarily.
            bookDao.markAsSynced(book.id) // This sets syncedToFirebase = 1 in the local database.

        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync book to Firebase: ${e.message}", e)

        }
    }

    // This function finds all unsynced local books and tries to upload them to Firestore.
    suspend fun syncUnsyncedBooks(): Int {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No internet - cannot sync")
            return 0 // This returns zero because we did not sync any items.
        }

        return try {
            Log.d(TAG, "Syncing all unsynced books...")

            val unsyncedBooks = bookDao.getUnsyncedBooks() // This loads all rows where syncedToFirebase = 0.
            Log.d(TAG, "Found ${unsyncedBooks.size} unsynced books")

            unsyncedBooks.forEach { book -> //  loops through every unsynced book.
                syncBookToFirebase(book) // marks rows as synced on success.
            }

            unsyncedBooks.size // This returns the number of items we attempted to sync.

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing unsynced books: ${e.message}", e)
            0 // This returns zero because the operation failed.
        }
    }

    // This function downloads all cloud books for the current user and merges them into the local database.
    suspend fun fetchFavoritesFromFirebase(): Int { // This returns how many books we brought down.
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No internet - cannot fetch from Firebase")
            return 0
        }

        // Get the current user's ID, if they are logged in.
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot fetch from Firebase: No user is logged in.")
            return 0
        }

        return try {
            Log.d(TAG, "Fetching favorites from Firebase...")

            // This queries Firestore for all book documents under this user’s sub-collection.
            val snapshot = booksCollection
                .document(userId)
                .collection("userBooks")
                .get() // This asks Firestore to get all documents in that collection.
                .await()

            Log.d(TAG, "Found ${snapshot.documents.size} books in Firebase")

            // This converts Firestore documents into our local BookEntity list
            val firebaseBooks = snapshot.documents.mapNotNull { doc -> // This turns each doc into an entity or null to skip.
                try {
                    BookEntity(
                        id = doc.getString("id") ?: return@mapNotNull null,
                        title = doc.getString("title") ?: "",
                        author = doc.getString("author") ?: "",
                        year = doc.getLong("year")?.toInt(),
                        coverUrl = doc.getString("coverUrl"),
                        personalPhotoPath = doc.getString("personalPhotoPath"),
                        isManualEntry = doc.getBoolean("isManualEntry") ?: false
                        ,
                        dateAdded = doc.getLong("dateAdded") ?: System.currentTimeMillis(),
                        syncedToFirebase = true
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing book from Firebase: ${e.message}")
                    null
                }
            }

            if (firebaseBooks.isNotEmpty()) {
                bookDao.insertBooks(firebaseBooks)
                Log.d(TAG, "Inserted ${firebaseBooks.size} books from Firebase") // This logs how many were merged locally.
            }

            firebaseBooks.size

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Firebase: ${e.message}", e)
            0
        }
    }
}
