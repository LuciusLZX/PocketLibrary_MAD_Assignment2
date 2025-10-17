// viewmodel/LibraryViewModel.kt
package com.example.pocketlibrary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel           // ViewModel that can access an Application context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope            // Coroutine scope tied to ViewModel lifecycle
import com.example.pocketlibrary.data.local.entity.BookEntity
import com.example.pocketlibrary.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow     // Mutable, observable state holder (Flow style)
import kotlinx.coroutines.flow.StateFlow            // Read-only view of a MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow          // Expose read-only version
import kotlinx.coroutines.launch                    // Launch coroutines tied to the ViewModel

/**
 * ViewModel for the "My Library" screen.
 *
 *  it does:
 * - Talks to the repository to load/save/delete books.
 * - Holds all screen UI state (list, loading flags, messages).
 * - Exposes StateFlow so Compose can collect and recompose automatically.
 */
// Pass the same repository instance will ensure same shared instance for the rest of the app
class LibraryViewModel(
    private val bookRepository: BookRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // ===================== State (everything the screen needs to render) =====================

    /** The current search text for filtering local favorites. Empty means "show all". */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow() // expose read-only

    /** The current list of books to display . */
    private val _favoriteBooks = MutableStateFlow<List<BookEntity>>(emptyList())
    val favoriteBooks: StateFlow<List<BookEntity>> = _favoriteBooks.asStateFlow()

    /** General loading spinner for the list area. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Loading spinner for sync actions specifically  */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** error text  */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** success text  */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /** which book is “selected”  */
    private val _selectedBook = MutableStateFlow<BookEntity?>(null)
    val selectedBook: StateFlow<BookEntity?> = _selectedBook.asStateFlow()

    /** Network availability flag (so UI can show banners or disable actions). */
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    // ===================== Initialization (runs once when VM is created) =====================

    init {
        // Load the initial list (this starts collecting Room Flow and pushes into StateFlow).
        loadFavorites()

        // Snapshot network state one time ( also call this before sync/search).
        checkNetworkAvailability()

        // Try bringing down cloud copies right away (if online), silent if offline.
        syncFromFirebase()
    }

    // ===================== Public API the Composables call =====================

    /**
     * Loads favorites based on current searchQuery.
     */
    fun loadFavorites() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val query = _searchQuery.value.trim()
                val flow = if (query.isEmpty()) {
                    bookRepository.getAllFavorites()      // Flow<List<BookEntity>>
                } else {
                    bookRepository.searchFavorites(query)
                }
                flow.collect { books ->
                    _favoriteBooks.value = books
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error loading favorites: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /** Update the search query; reload the list to collect the correct Flow. */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        loadFavorites()
    }

    /** Clears the search and shows all favorites. */
    fun clearSearch() {
        _searchQuery.value = ""
        loadFavorites()
    }

    /**
     * Delete a book.
     * We:
     * 1) ask repository to delete locally (UI updates via Flow),
     * 2) attempt to delete in Firebase,
     * 3) surface a success/error message for a snackbar.
     */
    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            try {
                _successMessage.value = null
                _errorMessage.value = null

                val result = bookRepository.deleteBook(book)

                result.fold(
                    onSuccess = {
                        _successMessage.value = "Removed '${book.title}' from favorites"
                        if (_selectedBook.value?.id == book.id) _selectedBook.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to delete book"
                    }
                )

            } catch (e: Exception) {
                _errorMessage.value = "Error deleting book: ${e.message}"
            }
        }
    }

    /**
     * Update a book
     * If Room update succeeds, we sync that book to Firebase.
     */
    fun updateBook(book: BookEntity) {
        viewModelScope.launch {
            try {
                _successMessage.value = null
                _errorMessage.value = null

                val result = bookRepository.updateBook(book)

                result.fold(
                    onSuccess = { _successMessage.value = "Book updated successfully" },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to update book"
                    }
                )

            } catch (e: Exception) {
                _errorMessage.value = "Error updating book: ${e.message}"
            }
        }
    }

    /**
     * Add/replace the personal photo path for a book (after camera returns success).
     * Repository updates Room, then syncs Firestore if online.
     */
    fun addPersonalPhoto(bookId: String, photoPath: String) {
        viewModelScope.launch {
            try {
                _successMessage.value = null
                _errorMessage.value = null

                val result = bookRepository.updatePersonalPhoto(bookId, photoPath)

                result.fold(
                    onSuccess = { _successMessage.value = "Photo added successfully" },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to add photo"
                    }
                )

            } catch (e: Exception) {
                _errorMessage.value = "Error adding photo: ${e.message}"
            }
        }
    }

    /** Select  a book for detail purposes (optional future feature). */
    fun selectBook(book: BookEntity?) {
        _selectedBook.value = book
    }

    /**
     * Upload any unsynced local books to Firebase.
     * - Checks network (fails fast with a message if offline).
     * - Calls repository.syncUnsyncedBooks().
     * - Posts a short success message for the snackbar.
     */
    fun syncToFirebase() {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _successMessage.value = null
                _errorMessage.value = null

                checkNetworkAvailability()
                if (!_isNetworkAvailable.value) {
                    _errorMessage.value = "No internet connection"
                    _isSyncing.value = false
                    return@launch
                }

                val syncedCount = bookRepository.syncUnsyncedBooks()
                _successMessage.value =
                    if (syncedCount > 0) "Synced $syncedCount book(s) to cloud"
                    else "All books are already synced"

            } catch (e: Exception) {
                _errorMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Download all cloud books for this user and merge into Room.
     */
    fun syncFromFirebase() {
        viewModelScope.launch {
            try {
                _isSyncing.value = true

                checkNetworkAvailability()
                if (!_isNetworkAvailable.value) {
                    // On startup, we skip showing an error if offline (not critical).
                    _isSyncing.value = false
                    return@launch
                }

                val fetchedCount = bookRepository.fetchFavoritesFromFirebase()
                if (fetchedCount > 0) {
                    _successMessage.value = "Downloaded $fetchedCount book(s) from cloud"
                }

            } catch (e: Exception) {
                _errorMessage.value = null
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /** Quick check to update the network flag for the UI. */
    fun checkNetworkAvailability() {
        _isNetworkAvailable.value = bookRepository.isNetworkAvailable()
    }

    /** Clear one-off error so it doesn’t re-show. */
    fun clearError() { _errorMessage.value = null }

    /** Clear one-off success so it doesn’t re-show. */
    fun clearSuccess() { _successMessage.value = null }

    /** reload list and try a cloud fetch. */
    fun refresh() {
        loadFavorites()
        syncFromFirebase()
    }
}

