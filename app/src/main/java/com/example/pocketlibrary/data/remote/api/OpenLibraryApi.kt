package com.example.pocketlibrary.data.remote.api
import com.example.pocketlibrary.data.remote.model.OpenLibraryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit interface for Open Library API.
// Base URL: https://openlibrary.org/
// This interface declares what HTTP calls we can make

interface OpenLibraryApi {

    // Basic search
    // Example of a real request URL that Retrofit will build
    // https://openlibrary.org/search.json?q=jane%20austen&fields=key,title,author_name,first_publish_year,cover_i&limit=20

    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String, //  user’s search text.
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,cover_i",
        @Query("limit") limit: Int = 20 // limits how many results the server returns, the default here we set is 20.
    ): Response<OpenLibraryResponse> //  returns a Retrofit Response that wraps an OpenLibraryResponse body (or an error).
}

