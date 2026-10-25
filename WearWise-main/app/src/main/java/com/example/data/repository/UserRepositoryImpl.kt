package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.UserProfile
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val database: AppDatabase
) : UserRepository {

    override fun getUserProfile(): Flow<UserProfile?> {
        return database.userProfileDao().getUserProfile().map { it?.toDomain() }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        database.userProfileDao().insertOrUpdateProfile(profile.toEntity())
    }

    override suspend fun resetLearnedPreferences() {
        val current = database.userProfileDao().getUserProfile()
        // Reset to default baseline preferences
        val baseline = UserProfile(
            id = 1L,
            firstName = "Yared",
            nickname = "Dr. Yared",
            email = "yared@institution.edu",
            profession = "Lecturer & Researcher",
            dressEnvironment = "Smart Casual to Business Casual"
        )
        database.userProfileDao().insertOrUpdateProfile(baseline.toEntity())
    }
}
