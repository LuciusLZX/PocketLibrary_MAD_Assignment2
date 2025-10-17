package com.example.pocketlibrary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketlibrary.data.local.entity.BookEntity
import com.example.pocketlibrary.data.remote.model.BookDoc
import com.example.pocketlibrary.data.repository.AuthRepository
import com.example.pocketlibrary.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Book Search Screen.
 *
 * - Holds the current search query and the list of results.
 * - Calls the Repository to hit the Open Library API.
 * - Exposes loading and error messages for the UI to show spinners/snackbars.
 * - Adds a book to favorites by asking the Repository.
 * - Survives rotation (so results don’t disappear on screen rotation).
 */
// Each view model will pass the same object instance of the repository, to keep track of the logged in user.
class SearchViewModel(
    private val bookRepository: BookRepository, // This is most important as it will say "Which user am I"
    private val authRepository: AuthRepository
) : ViewModel() {

    // ============ State with StateFlow ============

    /** What the user typed in the search box. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** The list of results returned from the network API (Open Library). */
    private val _searchResults = MutableStateFlow<List<BookDoc>>(emptyList())
    val searchResults: StateFlow<List<BookDoc>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** One-shot error message (null when there is no error to show). */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** One-shot success message (e.g., after adding to favorites). */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /** True/false flag so the UI can show “offline” banners. */
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    /**
     * IDs of books already saved in favorites.
     * The UI uses this to disable the “Add” button for those entries.
     */
    private val _favoriteBookIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteBookIds: StateFlow<Set<String>> = _favoriteBookIds.asStateFlow()

    // ============ Init ============
    init {
        // On screen creation, we check network so the UI can display a banner if offline.
        checkNetworkAvailability()
    }

    // ============ Public API for the UI ============

    /** Update the search text as the user types. */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Perform the online search (called when user presses Search).
     */
    fun searchBooks() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) {
            _errorMessage.value = "Please enter a search term"
            return
        }

        _searchResults.value = emptyList()              // clear old results
        _errorMessage.value = null                      // clear old error
        _successMessage.value = null                    // clear old success

        checkNetworkAvailability()                      // update network state flag

        viewModelScope.launch {
            try {
                _isLoading.value = true                 // show spinner

                val result = bookRepository.searchBooksOnline(query)

                result.fold(
                    onSuccess = { books ->
                        _searchResults.value = books               // update list
                        if (books.isEmpty()) {
                            _errorMessage.value = "No books found for '$query'" // friendly empty state message
                        }
                        // Also compute which results are already in favorites so UI can disable “Add”
                        loadFavoriteStatus(books)
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Search failed"
                        _searchResults.value = emptyList()
                    }
                )

            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add one result item into favorites.
     */
    fun addBookToFavorites(bookDoc: BookDoc) {
        viewModelScope.launch {
            try {
                _successMessage.value = null            // reset messages
                _errorMessage.value = null

                val result = bookRepository.addBookToFavorites(bookDoc)

                result.fold(
                    onSuccess = { savedBook ->
                        // Update the set so the “Add” button turns into “In Library”
                        _favoriteBookIds.value = _favoriteBookIds.value + savedBook.id
                        _successMessage.value = "Added '${savedBook.title}' to favorites"
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to add book"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error adding book: ${e.message}"
            }
        }
    }

    /** Clear UI state when user taps “Clear Search”. */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _errorMessage.value = null
        _successMessage.value = null
    }

    /** After showing an error snackbar, the screen calls this to clear it. */
    fun clearError() {
        _errorMessage.value = null
    }

    /** After showing a success snackbar, the screen calls this to clear it. */
    fun clearSuccess() {
        _successMessage.value = null
    }

    /** Ask Repository if we’re online right now (so UI can show a banner). */
    fun checkNetworkAvailability() {
        _isNetworkAvailable.value = bookRepository.isNetworkAvailable()
    }

    // ============ Private helpers ============

    /**
     * Given the list of results, check which are already in favorites.
     */
    private fun loadFavoriteStatus(books: List<BookDoc>) {
        viewModelScope.launch {
            try {
                val favoriteIds = books.mapNotNull { book ->
                    if (bookRepository.isBookInFavorites(book.key)) book.key else null
                }.toSet()
                _favoriteBookIds.value = favoriteIds
            } catch (_: Exception) {
            }
        }
    }
}
