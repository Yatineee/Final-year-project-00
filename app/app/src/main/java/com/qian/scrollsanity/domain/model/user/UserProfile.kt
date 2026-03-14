package com.qian.scrollsanity.domain.model.user

import com.google.firebase.firestore.DocumentId

//data class UserProfile(
//    @DocumentId
//    val uid: String = "",
//    val email: String = "",
//    val displayName: String? = null,
//    //my logic
//    val nickname: String? = "",
//    @ServerTimestamp
//    val onboardingCompleted: Boolean = false,
//    // my logic
//    val createdAt: Long = System.currentTimeMillis(),
//    val updatedAt: Long = System.currentTimeMillis()
////    val createdAt: Date? = null
//)



data class UserProfile(

    @DocumentId
    val uid: String = "",

    val email: String = "",
    val displayName: String? = null,

    val nickname: String? = null,

    val onboardingCompleted: Boolean = false,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)