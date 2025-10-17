package com.example.pocketlibrary.data.local.dao
import androidx.room.*
import com.example.pocketlibrary.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

// This interface defines how we talk to the database about book(s) object.
@Dao
interface BookDao {

    // This method inserts one BookEntity into the database and returns the new row ID.
    // If a book with the same primary key already exists, it will replace the old row.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    // This method inserts a list of books in one go which is useful for batch operations like syncing.
    // It also replaces existing rows that have the same primary key.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    // This method updates a single existing book and returns how many rows were changed (usually 1).
    @Update
    suspend fun updateBook(book: BookEntity): Int

    // This method deletes the exact BookEntity record we pass in.
    @Delete
    suspend fun deleteBook(book: BookEntity)

    // This method returns a Flow stream of all books sorted by newest first so the UI updates automatically.
    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    // This method searches books by title or author without caring about uppercase or lowercase letters.
    @Query(""" 
        SELECT * FROM books 
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%' 
        OR LOWER(author) LIKE '%' || LOWER(:query) || '%'
        ORDER BY dateAdded DESC
    """)
    fun searchBooks(query: String): Flow<List<BookEntity>>

    // This method fetches exactly one book by its id and returns null if it does not exist.
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    // This method checks if a book exists and returns true or false.
    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE id = :bookId LIMIT 1)")
    suspend fun isBookInFavorites(bookId: String): Boolean

    // This method gets all books that have not been synced to Firebase yet so we can upload them later.
    // SQL filters rows where syncedToFirebase is false (0).
    @Query("SELECT * FROM books WHERE syncedToFirebase = 0")
    suspend fun getUnsyncedBooks(): List<BookEntity>

    // This method marks a single book as synced after a successful Firebase upload.
    @Query("UPDATE books SET syncedToFirebase = 1 WHERE id = :bookId")
    suspend fun markAsSynced(bookId: String)

    // This method updates the local photo path for a book after the user attaches a personal image.
    @Query("UPDATE books SET personalPhotoPath = :photoPath WHERE id = :bookId")
    suspend fun updatePersonalPhoto(bookId: String, photoPath: String)

    // This method returns how many books are stored.
    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    // This method deletes all rows in the books table.
    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()
}

