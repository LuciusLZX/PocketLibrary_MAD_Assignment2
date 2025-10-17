// util/FirebaseTest.kt
package com.example.pocketlibrary.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firebase Test Utility
 * - A tiny “sanity check” you can run at app startup
 *   to confirm that Firebase Auth and Firestore are correctly configured.
 */
object FirebaseTest {

    private const val TAG = "FirebaseTest" // Used to filter logs in Logcat

    /**
     * Test Firebase Authentication (Anonymous)
     */
    suspend fun testAuthentication(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance() // Get Auth singleton for this process

            // If no user is signed in yet, create an anonymous account.
            // Anonymous auth gives us a stable UID for Firestore documents.
            if (auth.currentUser == null) {
                Log.d(TAG, "No user signed in. Signing in anonymously...")
                auth.signInAnonymously().await()
            }

            // Read back the UID to confirm we have a valid user
            val userId = auth.currentUser?.uid
            Log.d(TAG, "Authentication SUCCESS - User ID: $userId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Authentication FAILED: ${e.message}", e)
            false
        }
    }

    /**
     * Test Firestore Database Connection
     *
     *  it does:
     * - Ensures we have a signed-in user (anonymous if needed).
     * - Writes a small test document under /books/{uid}/userBooks/test_book.
     * - Reads that document back, logs its data.
     * - Deletes the test document (cleanup).
     */
    suspend fun testFirestore(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()                 // Auth client
            val firestore = FirebaseFirestore.getInstance()       // Firestore client

            // Make sure we have a user (anonymous is fine for tests)
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }

            val userId = auth.currentUser?.uid ?: throw Exception("No user ID")

            // Minimal test payload so we can see it in the console as well
            val testData = hashMapOf(
                "test" to "Firebase connection successful!",
                "timestamp" to System.currentTimeMillis()
            )

            // WRITE: /books/{userId}/userBooks/test_book
            firestore.collection("books")
                .document(userId)
                .collection("userBooks")
                .document("test_book")
                .set(testData)            // upsert test document
                .await()                  // suspend until write completes

            Log.d(TAG, "✅ Firestore WRITE SUCCESS")

            // READ the same doc back
            val snapshot = firestore.collection("books")
                .document(userId)
                .collection("userBooks")
                .document("test_book")
                .get()
                .await()

            Log.d(TAG, "Firestore READ SUCCESS: ${snapshot.data}")

            // DELETE the test doc
            firestore.collection("books")
                .document(userId)
                .collection("userBooks")
                .document("test_book")
                .delete()
                .await()

            Log.d(TAG, "Firestore DELETE SUCCESS")

            true

        } catch (e: Exception) {
            Log.e(TAG, "Firestore FAILED: ${e.message}", e)
            false
        }
    }

    /**
     * Run both tests (Auth then Firestore) and print a banner result.
     */
    suspend fun runAllTests(): Boolean {
        Log.d(TAG, "========== FIREBASE TESTS START ==========")

        val authSuccess = testAuthentication()
        val firestoreSuccess = testFirestore()

        val allSuccess = authSuccess && firestoreSuccess

        if (allSuccess) {
            Log.d(TAG, "========== ALL TESTS PASSED ==========")
        } else {
            Log.e(TAG, "========== SOME TESTS FAILED ==========")
        }

        return allSuccess
    }
}
