package com.example.pocketlibrary.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.lang.Exception

/**
 * This repository manage any authentication procedures with Firebase,
 * allowing users to sign up, log in, and log out anytime. This will
 * ensure a Firebase content to persist in multiple devices.
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser = auth.currentUser

    /** This will allow user to create a new Firebase account, if none. */
    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            // Return the user ID on success, or an empty string if it's somehow null
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Allow users to key in their credentials to log into their Firebase account. */
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Logout current user. */
    fun logout() {
        auth.signOut()
    }
}
