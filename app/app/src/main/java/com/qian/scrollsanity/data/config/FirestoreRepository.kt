package com.qian.scrollsanity.data.config

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.qian.scrollsanity.data.onboarding.OnboardingInput
import com.qian.scrollsanity.domain.model.usagedata.AppUsageData
import com.qian.scrollsanity.domain.model.user.GoalItem
import com.qian.scrollsanity.domain.model.user.InterestItem
import com.qian.scrollsanity.domain.model.user.UserPreferences
import com.qian.scrollsanity.domain.model.user.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "FirestoreRepository"

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val DATA_SUBCOLLECTION = "data"

        private const val PROFILE_DOCUMENT = "profile"
        private const val PREFERENCES_DOCUMENT = "preferences"

        private const val GOALS_COLLECTION = "goals"
        private const val INTERESTS_COLLECTION = "interests"
        private const val APPS_COLLECTION = "apps"

        private const val FIELD_INTENSITY = "interventionIntensity"
        private const val FIELD_TONE_STYLE = "toneStyle"
        private const val FIELD_UPDATED_AT = "updatedAt"
    }

    // =====================================================
    // USER PROFILE
    // =====================================================

    suspend fun createUserProfile(
        userId: String,
        email: String,
        displayName: String? = null,
        onboardingCompleted: Boolean = false,
        nickname: String? = null
    ): Result<Unit> = try {
        val data = mutableMapOf<String, Any>(
            "uid" to userId,
            "email" to email,
            "onboardingCompleted" to onboardingCompleted,
            "updatedAt" to System.currentTimeMillis(),
            "createdAt" to System.currentTimeMillis()
        )

        displayName?.let { data["displayName"] = it }
        nickname?.let { data["nickname"] = it }

        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)
            .set(data, SetOptions.merge())
            .await()

        Log.d(tag, "User profile created/merged for: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to create user profile", e)
        Result.failure(e)
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> = try {
        val doc = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)
            .get()
            .await()

        Result.success(docToUserProfile(doc))
    } catch (e: Exception) {
        Log.e(tag, "Failed to get user profile", e)
        Result.failure(e)
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing profile", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.let { docToUserProfile(it) })
            }

        awaitClose { registration.remove() }
    }

    suspend fun updateNickname(
        userId: String,
        nickname: String?
    ): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)
            .set(
                mapOf(
                    "nickname" to nickname,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to update nickname", e)
        Result.failure(e)
    }

    suspend fun updateDisplayName(
        userId: String,
        displayName: String?
    ): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)
            .set(
                mapOf(
                    "displayName" to displayName,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to update displayName", e)
        Result.failure(e)
    }

    // =====================================================
    // USER PREFERENCES
    // =====================================================

    suspend fun syncPreferences(
        userId: String,
        preferences: UserPreferences
    ): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .set(preferences, SetOptions.merge())
            .await()

        Log.d(tag, "Preferences synced for: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to sync preferences", e)
        Result.failure(e)
    }

    suspend fun getPreferences(userId: String): Result<UserPreferences?> = try {
        val doc = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .get()
            .await()

        Result.success(doc.toObject(UserPreferences::class.java))
    } catch (e: Exception) {
        Log.e(tag, "Failed to get preferences", e)
        Result.failure(e)
    }

    fun observePreferences(userId: String): Flow<UserPreferences?> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing preferences", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserPreferences::class.java))
            }

        awaitClose { registration.remove() }
    }

    suspend fun updateInterventionIntensity(
        userId: String,
        intensity: String
    ): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .set(
                mapOf(
                    FIELD_INTENSITY to intensity.uppercase(),
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to update intervention intensity", e)
        Result.failure(e)
    }

    suspend fun updateToneStyle(
        userId: String,
        toneStyle: String
    ): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .set(
                mapOf(
                    FIELD_TONE_STYLE to toneStyle,
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to update toneStyle", e)
        Result.failure(e)
    }

    // =====================================================
    // ONBOARDING
    // Atomic commit + seed goals + seed interests
    // =====================================================

    suspend fun completeOnboarding(
        userId: String,
        input: OnboardingInput
    ): Result<Unit> = try {
        val profileRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)

        val preferencesRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)

        val cleanedGoals = input.goals
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val cleanedInterests = input.interests
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val goalRefs = cleanedGoals.map {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(GOALS_COLLECTION)
                .document()
        }

        val interestRefs = cleanedInterests.map {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(INTERESTS_COLLECTION)
                .document()
        }

        val tone = input.toneStyle.ifBlank { "gentle" }
        val intensity = input.interventionIntensity.ifBlank { "MEDIUM" }.uppercase()

        val now = System.currentTimeMillis()

        firestore.runBatch { batch ->
            val profileMap = mutableMapOf<String, Any>(
                "nickname" to input.nickname,
                "onboardingCompleted" to true,
                "updatedAt" to now
            )
            input.displayName?.let { profileMap["displayName"] = it }

            batch.set(profileRef, profileMap, SetOptions.merge())

            batch.set(
                preferencesRef,
                mapOf(
                    FIELD_INTENSITY to intensity,
                    FIELD_TONE_STYLE to tone,
                    FIELD_UPDATED_AT to now
                ),
                SetOptions.merge()
            )

            cleanedGoals.zip(goalRefs).forEach { (goalText, ref) ->
                batch.set(
                    ref,
                    mapOf(
                        "text" to goalText,
                        "status" to "active",
                        "createdAtMs" to now,
                        "createdAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }

            cleanedInterests.zip(interestRefs).forEach { (interestText, ref) ->
                batch.set(
                    ref,
                    mapOf(
                        "text" to interestText,
                        "status" to "active",
                        "createdAtMs" to now,
                        "createdAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to complete onboarding", e)
        Result.failure(e)
    }

    // =====================================================
    // GOALS
    // users/{uid}/goals/{goalId}
    // =====================================================

    fun observeGoals(userId: String): Flow<List<GoalItem>> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(GOALS_COLLECTION)
            .orderBy("createdAtMs", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing goals", error)
                    close(error)
                    return@addSnapshotListener
                }

                val goals = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(GoalItem::class.java)?.copy(id = doc.id)
                }.orEmpty()

                trySend(goals)
            }

        awaitClose { registration.remove() }
    }

    suspend fun addGoal(userId: String, text: String): Result<Unit> {
        return try {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("Goal text is empty"))
            }

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(GOALS_COLLECTION)
                .document()
                .set(
                    mapOf(
                        "text" to trimmed,
                        "status" to "active",
                        "createdAtMs" to System.currentTimeMillis(),
                        "createdAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add goal", e)
            Result.failure(e)
        }
    }

    suspend fun setGoalAchieved(
        userId: String,
        goalId: String,
        achieved: Boolean
    ): Result<Unit> = try {
        val ref = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(GOALS_COLLECTION)
            .document(goalId)

        if (achieved) {
            ref.set(
                mapOf(
                    "status" to "achieved",
                    "achievedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        } else {
            ref.set(
                mapOf(
                    "status" to "active",
                    "achievedAt" to null
                ),
                SetOptions.merge()
            ).await()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to update goal achieved", e)
        Result.failure(e)
    }

    suspend fun deleteGoal(userId: String, goalId: String): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(GOALS_COLLECTION)
            .document(goalId)
            .delete()
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to delete goal", e)
        Result.failure(e)
    }

    // =====================================================
    // INTERESTS
    // users/{uid}/interests/{interestId}
    // =====================================================

    fun observeInterests(userId: String): Flow<List<InterestItem>> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(INTERESTS_COLLECTION)
            .orderBy("createdAtMs", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing interests", error)
                    close(error)
                    return@addSnapshotListener
                }

                val interests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InterestItem::class.java)?.copy(id = doc.id)
                }.orEmpty()

                trySend(interests)
            }

        awaitClose { registration.remove() }
    }

    suspend fun addInterest(userId: String, text: String): Result<Unit> {
        return try {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("Interest text is empty"))
            }

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(INTERESTS_COLLECTION)
                .document()
                .set(
                    mapOf(
                        "text" to trimmed,
                        "status" to "active",
                        "createdAtMs" to System.currentTimeMillis(),
                        "createdAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add interest", e)
            Result.failure(e)
        }
    }

    suspend fun setInterestAchieved(
        userId: String,
        interestId: String,
        achieved: Boolean
    ): Result<Unit> = try {
        val ref = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(INTERESTS_COLLECTION)
            .document(interestId)

        if (achieved) {
            ref.set(
                mapOf(
                    "status" to "achieved",
                    "achievedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        } else {
            ref.set(
                mapOf(
                    "status" to "active",
                    "achievedAt" to null
                ),
                SetOptions.merge()
            ).await()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to update interest achieved", e)
        Result.failure(e)
    }

    suspend fun deleteInterest(userId: String, interestId: String): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(INTERESTS_COLLECTION)
            .document(interestId)
            .delete()
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to delete interest", e)
        Result.failure(e)
    }

    // =====================================================
    // DELETE USER DATA
    // profile / preferences / goals / interests
    // =====================================================

    suspend fun deleteUserData(userId: String): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .delete()
            .await()

        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)
            .delete()
            .await()

        val goalsSnap = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(GOALS_COLLECTION)
            .get()
            .await()

        val interestsSnap = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(INTERESTS_COLLECTION)
            .get()
            .await()

        firestore.runBatch { batch ->
            goalsSnap.documents.forEach { batch.delete(it.reference) }
            interestsSnap.documents.forEach { batch.delete(it.reference) }
        }.await()

        Log.d(tag, "User data deleted for: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(tag, "Failed to delete user data", e)
        Result.failure(e)
    }

    // =====================================================
    // APP USAGE STATISTICS
    // users/{uid}/apps/{appId}
    // =====================================================

    suspend fun incrementAppUsage(
        userId: String,
        appId: String,
        deltaSeconds: Long,
        date: String
    ): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(APPS_COLLECTION)
            .document(appId)
            .set(
                mapOf(
                    "date" to date,
                    "secondsToday" to FieldValue.increment(deltaSeconds),
                    "lastUpdated" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAllAppUsageToday(userId: String): Result<List<AppUsageData>> = try {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val docs = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(APPS_COLLECTION)
            .whereEqualTo("date", dateStr)
            .get()
            .await()

        Result.success(
            docs.documents.mapNotNull { it.toObject(AppUsageData::class.java) }
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    // =====================================================
    // MAPPERS
    // =====================================================

    private fun docToUserProfile(doc: DocumentSnapshot): UserProfile? {
        if (!doc.exists()) return null

        fun anyToLong(value: Any?): Long {
            return when (value) {
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                is Timestamp -> value.toDate().time
                is Date -> value.time
                else -> 0L
            }
        }

        val data = doc.data ?: emptyMap<String, Any?>()

        return UserProfile(
            uid = (data["uid"] as? String).orEmpty().ifBlank { doc.id },
            email = (data["email"] as? String).orEmpty(),
            displayName = data["displayName"] as? String,
            nickname = data["nickname"] as? String,
            onboardingCompleted = data["onboardingCompleted"] as? Boolean ?: false,
            createdAt = anyToLong(data["createdAt"]),
            updatedAt = anyToLong(data["updatedAt"])
        )
    }
}