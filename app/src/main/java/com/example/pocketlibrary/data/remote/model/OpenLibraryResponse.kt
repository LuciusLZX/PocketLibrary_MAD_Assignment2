package com.example.pocketlibrary.data.remote.model
import com.squareup.moshi.Json

// this is the returned API response wrapper for Open Library search endpoint
// The server returns a JSON object that contains numFound, docs[]
// JSON docs[] array → list of BookDoc.

data class OpenLibraryResponse(
    @Json(name = "docs")
    val docs: List<BookDoc>, //  holds the list of search results

    @Json(name = "numFound")
    val numFound: Int, // total number of results that the server found

    @Json(name = "start")
    val start: Int = 0, // starting index for pagination, the default here is 0.

    @Json(name = "numFoundExact")
    val numFoundExact: Boolean = true //whether numFound is exact or estimated, default true.
)


// Represents a single book entry (one item in docs[]).
// Each property maps to a field in the Open Library JSON.
data class BookDoc(
    @Json(name = "key")
    val key: String,

    @Json(name = "title")
    val title: String,

    @Json(name = "author_name")
    val authorName: List<String>? = null,

    @Json(name = "first_publish_year")
    val firstPublishYear: Int? = null,

    @Json(name = "cover_i")
    val coverId: Long? = null,

    @Json(name = "isbn")
    val isbn: List<String>? = null,

    @Json(name = "publisher")
    val publisher: List<String>? = null,

    @Json(name = "language")
    val language: List<String>? = null
) {
    // This function builds a cover image URL using the coverId and a size (S, M, or L).
    fun getCoverImageUrl(size: String = "M"): String? =
        coverId?.let { "https://covers.openlibrary.org/b/id/$it-$size.jpg" }
    // If coverId is null, return null, else build the URL.

    // This function joins the author list into a single string, or returns "Unknown Author" if the list is empty or null.
    fun getAuthorsString(): String =
        if (authorName.isNullOrEmpty()) "Unknown Author" else authorName.joinToString(", ")

    // This function returns only the first author’s name, or "Unknown Author" if none exists.
    fun getFirstAuthor(): String =
        authorName?.firstOrNull() ?: "Unknown Author"

    // This function returns true if the book has a coverId; otherwise false.
    fun hasCover(): Boolean = coverId != null

    // This function converts the network model (BookDoc) into local Room entity (BookEntity) so it can be saved offline.
    fun toBookEntity(): com.example.pocketlibrary.data.local.entity.BookEntity {
        return com.example.pocketlibrary.data.local.entity.BookEntity(
            id = key,
            title = title,
            author = getAuthorsString(),
            year = firstPublishYear,
            coverUrl = getCoverImageUrl("L"),
            personalPhotoPath = null,
            isManualEntry = false,
            dateAdded = System.currentTimeMillis(),
            syncedToFirebase = false
        )
    }
}

