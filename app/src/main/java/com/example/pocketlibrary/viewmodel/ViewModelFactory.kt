package com.example.pocketlibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pocketlibrary.data.repository.AuthRepository
import com.example.pocketlibrary.data.repository.BookRepository

/** This class acts as a factory for creating all the view models (search/ library/ manual entry)
 * to have the same instances of the repository created in MainActivity, ensuring a single source
 * of truth, allowing the data of this logged in user to persist across different screens/ devices.
 *
 * Step-by-step walkthrough:
 * 1. MainActivity create the repositories and this factory.
 * 2. This factory will be used to fulfill any viewModel request by UI.
 * 3. The factory storing the created repositories instance will produce viewModels with the same repositories instances. */
class ViewModelFactory(
    private val bookRepository: BookRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    // This method will be called by the Android system whenever creating a ViewModel.
    override fun <T : ViewModel> create(modelClass: Class<T>): T { // Create any type of ViewModel by reading the actual class type (e.g. SearchViewModel::class.java).

        // Creates a SearchViewModel
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") // Suppress casting warning, since this is safe
            return SearchViewModel(bookRepository, authRepository) as T // Type cast to a generic type before returning it
        }

        // Creates a LibraryViewModel
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(bookRepository, authRepository) as T
        }

        // Creates a ManualEntryViewModel
        if (modelClass.isAssignableFrom(ManualEntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManualEntryViewModel(bookRepository, authRepository) as T
        }

        // Check for any unknown classes during creation
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
