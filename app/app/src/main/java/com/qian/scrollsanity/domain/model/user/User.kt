package com.qian.scrollsanity.domain.model.user

data class User(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
