package com.example.pocketlibrary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel          // A ViewModel that can access the Application context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope           // Coroutine scope tied to the ViewModel lifecycle
import com.example.pocketlibrary.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow    // A mutable, observable state holder (Flow)
import kotlinx.coroutines.flow.StateFlow           // Read-only interface for exposing state to UI
import kotlinx.coroutines.flow.asStateFlow         // Converts MutableStateFlow -> StateFlow (read-only view)
import kotlinx.coroutines.launch                   // Launch coroutines inside the ViewModel

/**
 * ViewModel for the “Manual Book Entry” screen.
 * - It stores the text the user types (title, author, year).
 * - It validates the text
 * - It asks the Repository to save the book when the user presses “Add”.
 * - It exposes success/error/loading so the screen can show snackbars/spinners.
 */
class ManualEntryViewModel(
    private val bookRepository: BookRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // ======================== Input fields state ========================
    // Each field is a MutableStateFlow so the UI can observe changes
    // and recompose immediately as the user types.

    /** The text the user typed for the book title. */
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    /** The text the user typed for the author. */
    private val _author = MutableStateFlow("")
    val author: StateFlow<String> = _author.asStateFlow()

    /**
     * The text the user typed for the year.
     * We keep it as String because TextField gives us strings,
     * and only convert to Int at the very end (if not empty).
     */
    private val _year = MutableStateFlow("")
    val year: StateFlow<String> = _year.asStateFlow()

    // ======================== Validation state ========================
    // Each error is nullable. When null => no error. When non-null => show the message under the field.

    /** Error for the title field (null means OK). */
    private val _titleError = MutableStateFlow<String?>(null)
    val titleError: StateFlow<String?> = _titleError.asStateFlow()

    /** Error for the author field (null means OK). */
    private val _authorError = MutableStateFlow<String?>(null)
    val authorError: StateFlow<String?> = _authorError.asStateFlow()

    /** Error for the year field (null means OK). */
    private val _yearError = MutableStateFlow<String?>(null)
    val yearError: StateFlow<String?> = _yearError.asStateFlow()

    // ======================== Operation state ========================
    // These control spinners/snackbars/navigation when saving.

    /** True while we are doing a long operation (e.g., inserting). */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Becomes true after a successful insert.
     * The screen can observe this and navigate back.
     */
    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    /** A one-off error message to show in a snackbar. */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** A one-off success message to show in a snackbar. */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // ======================== Field updaters ========================
    // These are called from TextFields’ onValueChange.
    // We also clear the error as soon as the user types again.

    /** Update title and clear its error if any. */
    fun updateTitle(value: String) {
        _title.value = value
        _titleError.value = null
    }

    /** Update author and clear its error if any. */
    fun updateAuthor(value: String) {
        _author.value = value
        _authorError.value = null
    }

    /** Update year and clear its error if any. */
    fun updateYear(value: String) {
        _year.value = value
        _yearError.value = null
    }

    // ======================== Validation ========================
    /**
     * Checks all inputs and sets error messages when something is wrong.
     * Returns true if everything looks good.
     */
    private fun validateInputs(): Boolean {
        var isValid = true

        // Title must not be empty or super short.
        val titleTrim = _title.value.trim()
        if (titleTrim.isEmpty()) {
            _titleError.value = "Title is required"
            isValid = false
        } else if (titleTrim.length < 2) {
            _titleError.value = "Title must be at least 2 characters"
            isValid = false
        }

        // Author must not be empty or super short.
        val authorTrim = _author.value.trim()
        if (authorTrim.isEmpty()) {
            _authorError.value = "Author is required"
            isValid = false
        } else if (authorTrim.length < 2) {
            _authorError.value = "Author must be at least 2 characters"
            isValid = false
        }

        // Year is optional, but if user typed something, it must be a sensible number.
        val yearStr = _year.value.trim()
        if (yearStr.isNotEmpty()) {
            val yearInt = yearStr.toIntOrNull()
            if (yearInt == null) {
                _yearError.value = "Year must be a number"
                isValid = false
            } else if (yearInt < 1000 || yearInt > 2100) {
                _yearError.value = "Year must be between 1000 and 2100"
                isValid = false
            }
        }

        return isValid
    }

    // ======================== Add action ========================
    /**
     * Called when the user taps “Add Book”.
     * Steps:
     * 1) Validate fields. If invalid, we stop and show inline errors.
     * 2) Launch a coroutine, set loading=true, and call the Repository.
     * 3) The Repository returns Kotlin Result:
     *    - onSuccess -> set success flags, clear form.
     *    - onFailure -> set error message for snackbar.
     */
    fun addBook() {
        // Step 1: Validate before doing any work.
        if (!validateInputs()) return

        // Step 2: Do the insert in a coroutine so UI stays responsive.
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val titleValue = _title.value.trim()
                val authorValue = _author.value.trim()
                val yearValue = _year.value.trim().toIntOrNull() // convert only if not empty

                // Ask the Repository to create a manual book in Room (and try to sync).
                val result = bookRepository.addManualBook(
                    title = titleValue,
                    author = authorValue,
                    year = yearValue
                )

                // Step 3: Handle Kotlin Result in a tidy way.
                result.fold(
                    onSuccess = { book ->
                        _isSuccess.value = true
                        _successMessage.value = "Added '${book.title}' to favorites"
                        clearForm()                                            // reset the fields
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to add book"
                    }
                )

            } catch (e: Exception) {
                _errorMessage.value = "Error adding book: ${e.message}"
            } finally {
                _isLoading.value = false // always stop the spinner
            }
        }
    }

    // ======================== Helpers ========================
    /** Wipe all input fields and all their error messages. */
    fun clearForm() {
        _title.value = ""
        _author.value = ""
        _year.value = ""
        _titleError.value = null
        _authorError.value = null
        _yearError.value = null
    }

    /** After the screen navigates back, reset the success flag & message. */
    fun resetSuccess() {
        _isSuccess.value = false
        _successMessage.value = null
    }

    /** Let the UI clear the current error snackbar. */
    fun clearError() {
        _errorMessage.value = null
    }
}
