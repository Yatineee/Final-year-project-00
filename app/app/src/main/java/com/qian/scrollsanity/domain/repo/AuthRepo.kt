package com.qian.scrollsanity.domain.repo

interface AuthRepo {
    fun getCurrentUserId(): String?
}