package com.qian.scrollsanity.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.qian.scrollsanity.data.config.FirestoreRepository
import com.qian.scrollsanity.domain.model.user.User
import com.qian.scrollsanity.domain.model.user.UserPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestoreRepo: FirestoreRepository = FirestoreRepository()

    /**
     * Register a new user with email and password
     */
    suspend fun registerWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Failed to create user"))

            // 1) create profile placeholder (NOT completed)
            firestoreRepo.createUserProfile(
                userId = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                displayName = firebaseUser.displayName,
                onboardingCompleted = false
            )

            // 2) default preferences placeholder
            val defaultPreferences = UserPreferences(
                toneStyle = "gentle"
            )
            firestoreRepo.syncPreferences(firebaseUser.uid, defaultPreferences)

            Result.success(firebaseUser.toUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login with email and password
     */
    suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.success(firebaseUser.toUser())
            } else {
                Result.failure(Exception("Failed to sign in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Get the currently logged-in user
     */
    fun getCurrentUser(): User? {
        return firebaseAuth.currentUser?.toUser()
    }

    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUser())
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    /**
     * Convert Firebase user to our User model
     */
    private fun FirebaseUser.toUser(): User {
        return User(
            uid = uid,
            email = email ?: "",
            displayName = displayName,
            createdAt = metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
    }
}