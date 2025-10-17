// ui/components/SearchBar.kt
package com.example.pocketlibrary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Reusable Search Bar components:
 * - SearchBar            : single text field with search icon, optional spinner, and clear button
 * - SearchBarWithButton  : text field + explicit "Search" button
 */

/* ──────────────────────────────────────────────────────────────────────────────
   SearchBar
   ────────────────────────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String = "Search books...",
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Controller that lets us programmatically hide the software keyboard.
    val keyboardController = LocalSoftwareKeyboardController.current

    val focusRequester = remember { FocusRequester() }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester), // Attach the focus handle to this field
        placeholder = {
            // Light grey hint text shown only when query is empty
            Text(text = placeholder)
        },
        leadingIcon = {
            // Static search icon at the start of the field
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            // Right-side icon area:
            // - if we're loading: show a small spinner
            // - else if text is non-empty, show a clear (X) button
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { // clearing resets to empty string
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                // This runs when user hits the "Search" action on the keyboard
                keyboardController?.hide() // Hide keyboard so they can see results
                onSearch()
            }
        ),
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )

}

/* ──────────────────────────────────────────────────────────────────────────────
   SearchBarWithButton
   A variant that puts an explicit "Search" button next to the field.
   ────────────────────────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWithButton(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    placeholder: String = "Search books...",
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Gap between field and button
    ) {
        // TEXT FIELD (flexible width)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),               // Take remaining width, button uses minimal
            placeholder = { Text(text = placeholder) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                // Show clear only when there is text to clear
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearch()
                }
            ),
            shape = MaterialTheme.shapes.large
        )

        // SEARCH BUTTON
        Button(
            onClick = {
                keyboardController?.hide()
                onSearch()
            },
            modifier = Modifier
                .height(56.dp)
                .widthIn(min = 56.dp),
            enabled = query.isNotEmpty() && !isLoading,
            contentPadding = PaddingValues(16.dp)
        ) {
            if (isLoading) {
                // Show a spinner (inside the button) to signal work in progress
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                // Otherwise, just show the search icon
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            }
        }
    }
}
