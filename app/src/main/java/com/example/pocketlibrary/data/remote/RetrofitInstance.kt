package com.example.pocketlibrary.data.remote

import com.example.pocketlibrary.data.remote.api.OpenLibraryApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit


//Creates one Retrofit client for the whole app (a singleton).
object RetrofitInstance {

    // This is the root URL for the Open Library API; every endpoint path is appended to this.
    private const val BASE_URL = "https://openlibrary.org/"

    // This interceptor logs requests and responses to help debug networking issues during development.
    // We set the level to BODY so we can see headers and JSON bodies
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // This builds a customized OkHttp client that Retrofit will use under the hood for all HTTP calls.
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)            // adds the logger so every request/response is printed to Logcat while debugging.
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json")   // This tells the server that we expect the response in JSON format.
                .addHeader("User-Agent", "PocketLibrary/1.0")
                .build()
            chain.proceed(request)

        }
        .retryOnConnectionFailure(true)
        .build()

    // This creates a Moshi instance that knows how to read and write Kotlin data classes from/to JSON strings.
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // This lazily constructs the Retrofit instance the first time we need it.
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)                             // This sets the base URL so Retrofit knows where to send requests.
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi)) // use Moshi to convert JSON - Kotlin object or another way round.
            .build()
    }

    // repositories can call api.searchBooks without caring about setup details.
    val api: OpenLibraryApi by lazy {
        retrofit.create(OpenLibraryApi::class.java)
    }
}

