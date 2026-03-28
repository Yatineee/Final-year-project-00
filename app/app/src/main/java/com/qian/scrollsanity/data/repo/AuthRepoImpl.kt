package com.qian.scrollsanity.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.domain.repo.AuthRepo

class AuthRepoImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepo {
    override fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
}