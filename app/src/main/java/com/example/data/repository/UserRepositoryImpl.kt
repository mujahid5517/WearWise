package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.UserProfile
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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
        val current = database.userProfileDao().getUserProfile().firstOrNull()?.toDomain() ?: return
        database.userProfileDao().insertOrUpdateProfile(current.copy(
            stylePreferences = emptyList(), favoriteColors = emptyList(),
            avoidedColors = emptyList(), commonOccasions = emptyList()
        ).toEntity())
    }
}
