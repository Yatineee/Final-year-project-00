package com.qian.scrollsanity.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.qian.scrollsanity.domain.model.GoalItem
import com.qian.scrollsanity.domain.model.UserPreferences
import com.qian.scrollsanity.domain.model.UserProfile
import com.qian.scrollsanity.domain.model.usagedata.AppUsageData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirestoreRepository"

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val DATA_SUBCOLLECTION = "data"

        private const val PROFILE_DOCUMENT = "profile"
        private const val PREFERENCES_DOCUMENT = "preferences"

        private const val GOALS_COLLECTION = "goals"

        // Preferences fields (统一命名)
        private const val FIELD_INTENSITY = "interventionIntensity"  // "LOW"|"MEDIUM"|"HIGH"
        private const val FIELD_TONE_STYLE = "toneStyle"             // "gentle"|...
        private const val FIELD_INTERESTS = "interests"              // List<String>
        private const val FIELD_RECENT_GOAL = "recentGoalContext"    // String?
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
        // ⚠️ 用 map 避免 null 覆盖已有值
        val data = mutableMapOf<String, Any>(
            "uid" to userId,
            "email" to email,
            "onboardingCompleted" to onboardingCompleted,
            "updatedAt" to System.currentTimeMillis()
        )
        // createdAt 只在首次创建时写入；merge 不会覆盖已存在的 createdAt
        data["createdAt"] = System.currentTimeMillis()

        displayName?.let { data["displayName"] = it }
        nickname?.let { data["nickname"] = it }

        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)
            .set(data, SetOptions.merge())
            .await()

        Log.d(TAG, "User profile created/merged for: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create user profile", e)
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
        Log.e(TAG, "Failed to get user profile", e)
        Result.failure(e)
    }
    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val reg = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing profile", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.let { docToUserProfile(it) })
            }

        awaitClose { reg.remove() }
    }

    suspend fun updateNickname(userId: String, nickname: String?): Result<Unit> = try {
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
        Log.e(TAG, "Failed to update nickname", e)
        Result.failure(e)
    }

    suspend fun updateDisplayName(userId: String, displayName: String?): Result<Unit> = try {
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
        Log.e(TAG, "Failed to update displayName", e)
        Result.failure(e)
    }

    // =====================================================
    // USER PREFERENCES
    // =====================================================

    suspend fun syncPreferences(userId: String, preferences: UserPreferences): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .set(preferences, SetOptions.merge())
            .await()

        Log.d(TAG, "Preferences synced for: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to sync preferences", e)
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
        Log.e(TAG, "Failed to get preferences", e)
        Result.failure(e)
    }

    fun observePreferences(userId: String): Flow<UserPreferences?> = callbackFlow {
        val reg = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing preferences", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserPreferences::class.java))
            }

        awaitClose { reg.remove() }
    }

    suspend fun updateInterventionIntensity(userId: String, intensity: String): Result<Unit> = try {
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
        Log.e(TAG, "Failed to update intervention intensity", e)
        Result.failure(e)
    }

    suspend fun updateToneStyle(userId: String, toneStyle: String): Result<Unit> = try {
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
        Log.e(TAG, "Failed to update toneStyle", e)
        Result.failure(e)
    }

    suspend fun updateInterests(userId: String, interests: List<String>): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .set(
                mapOf(
                    FIELD_INTERESTS to interests.map { it.trim() }.filter { it.isNotBlank() },
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update interests", e)
        Result.failure(e)
    }

    suspend fun updateRecentGoalContext(userId: String, recentGoalContext: String?): Result<Unit> = try {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .set(
                mapOf(
                    FIELD_RECENT_GOAL to recentGoalContext,
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update recentGoalContext", e)
        Result.failure(e)
    }

    // =====================================================
    // ONBOARDING (atomic commit + seed goal)
    // =====================================================

    suspend fun completeOnboarding(userId: String, input: OnboardingInput): Result<Unit> = try {
        val profileRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)

        val prefRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)

        // 方案A：seed 一个 goal 到 goals 子集合（可选）
        val seedText = (input.seedGoalText ?: input.goal).trim().takeIf { it.isNotBlank() }
        val seedGoalRef = if (seedText != null) {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(GOALS_COLLECTION)
                .document()
        } else null

        val tone = input.toneStyle.ifBlank { input.preferStyle.ifBlank { "gentle" } }
        val intensity = input.interventionIntensity.ifBlank { "MEDIUM" }.uppercase()
        val recentGoal = input.recentGoalContext?.takeIf { it.isNotBlank() } ?: input.goal.takeIf { it.isNotBlank() }

        firestore.runBatch { batch ->
            // profile：nickname + completed（displayName 如有也写）
            val profileMap = mutableMapOf<String, Any>(
                "nickname" to input.nickname,
                "onboardingCompleted" to true,
                "updatedAt" to System.currentTimeMillis()
            )
            input.displayName?.let { profileMap["displayName"] = it }
            batch.set(profileRef, profileMap, SetOptions.merge())

            // preferences：统一写入新字段
            batch.set(
                prefRef,
                mapOf(
                    "interventionIntensity" to intensity,
                    "toneStyle" to tone,
                    "interests" to input.interests.map { it.trim() }.filter { it.isNotBlank() },
                    "recentGoalContext" to recentGoal,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )

            // seed goal：写入 goals collection
            if (seedGoalRef != null && seedText != null) {
                batch.set(
                    seedGoalRef,
                    mapOf(
                        "text" to seedText,
                        "status" to "active",
                        "createdAtMs" to System.currentTimeMillis(),
                        "createdAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to complete onboarding", e)
        Result.failure(e)
    }

    // =====================================================
    // GOALS (Todo-style)
    // users/{uid}/goals/{goalId}
    // =====================================================

    fun observeGoals(userId: String): Flow<List<GoalItem>> = callbackFlow {
        val reg = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(GOALS_COLLECTION)
            .orderBy("createdAtMs", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing goals", error)
                    close(error)
                    return@addSnapshotListener
                }

                val goals = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(GoalItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(goals)
            }

        awaitClose { reg.remove() }
    }

    suspend fun addGoal(userId: String, text: String): Result<Unit> {
        return try {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Goal text is empty"))

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
            Log.e(TAG, "Failed to add goal", e)
            Result.failure(e)
        }
    }

    suspend fun setGoalAchieved(userId: String, goalId: String, achieved: Boolean): Result<Unit> = try {
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
        Log.e(TAG, "Failed to update goal achieved", e)
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
        Log.e(TAG, "Failed to delete goal", e)
        Result.failure(e)
    }

    // =====================================================
    // UTILITY - delete profile/preferences/goals
    // =====================================================

    suspend fun deleteUserData(userId: String): Result<Unit> = try {
        // Delete preferences
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PREFERENCES_DOCUMENT)
            .delete()
            .await()

        // Delete profile
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DATA_SUBCOLLECTION)
            .document(PROFILE_DOCUMENT)
            .delete()
            .await()

        // Delete goals
        val goalsSnap = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(GOALS_COLLECTION)
            .get()
            .await()

        firestore.runBatch { batch ->
            goalsSnap.documents.forEach { batch.delete(it.reference) }
        }.await()

        Log.d(TAG, "User data deleted for: $userId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to delete user data", e)
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

        firestore.collection("users")
            .document(userId)
            .collection("apps")
            .document(appId)
            .set(
                mapOf(
                    "date" to date,
                    "secondsToday" to com.google.firebase.firestore.FieldValue.increment(deltaSeconds),
                    "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .await()

        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAllAppUsageToday(
        userId: String
    ): Result<List<AppUsageData>> = try {

        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())

        val docs = firestore.collection("users")
            .document(userId)
            .collection("apps")
            .whereEqualTo("date", dateStr)
            .get()
            .await()

        Result.success(
            docs.documents.mapNotNull {
                it.toObject(AppUsageData::class.java)
            }
        )

    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun docToUserProfile(doc: com.google.firebase.firestore.DocumentSnapshot): UserProfile? {
        if (!doc.exists()) return null

        fun anyToLong(v: Any?): Long {
            return when (v) {
                is Long -> v
                is Int -> v.toLong()
                is Double -> v.toLong()
                is com.google.firebase.Timestamp -> v.toDate().time
                is java.util.Date -> v.time
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

