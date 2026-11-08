package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthenticationRepository
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class UserRepositoryImpl(
    private val database: AppDatabase,
    private val authenticationRepository: AuthenticationRepository
) : UserRepository {

    override fun getUserProfile(): Flow<UserProfile?> {
        val dao = database.userProfileDao()
        return authenticationRepository.state
            .onEach { state -> state.user?.let { syncProfileFromAuthentication(it) } }
            .combine(dao.observeProfiles()) { state, profiles ->
                val uid = state.user?.uid ?: return@combine null
                profiles.firstOrNull { it.ownerUid == uid }?.toDomain()
            }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        val uid = authenticationRepository.state.value.user?.uid
            ?: error("Cannot save a profile without an authenticated user.")
        val dao = database.userProfileDao()
        dao.claimLegacyProfile(uid)
        dao.insertOrUpdateProfile(profile.toEntity().copy(ownerUid = uid))
    }

    override suspend fun resetLearnedPreferences() {
        val user = authenticationRepository.state.value.user ?: return
        syncProfileFromAuthentication(user)
        val dao = database.userProfileDao()
        val current = dao.getUserProfileOnce(user.uid)?.toDomain() ?: return
        dao.insertOrUpdateProfile(current.copy(
            stylePreferences = emptyList(), favoriteColors = emptyList(),
            avoidedColors = emptyList(), commonOccasions = emptyList()
        ).toEntity().copy(ownerUid = user.uid))
    }

    private suspend fun syncProfileFromAuthentication(user: AuthenticatedUser) {
        val activeUser = authenticationRepository.state.value.user
            ?.takeIf { it.uid == user.uid }
            ?: return
        val dao = database.userProfileDao()
        dao.claimLegacyProfile(activeUser.uid)
        val existing = dao.getUserProfileOnce(activeUser.uid)
        val currentUser = authenticationRepository.state.value.user
            ?.takeIf { it.uid == activeUser.uid }
            ?: return
        val authenticatedName = currentUser.displayName?.trim()?.takeIf(String::isNotEmpty)
        val authenticatedEmail = currentUser.email?.trim()?.takeIf(String::isNotEmpty)
        val profileName = authenticatedName
            ?: remoteName(currentUser.profile).takeIf(String::isNotEmpty)
            ?: authenticatedEmail?.substringBefore('@').orEmpty()
        if (existing == null) {
            dao.insertOrUpdateProfile(
                (currentUser.profile ?: UserProfile()).copy(
                    firstName = profileName,
                    nickname = currentUser.profile?.nickname?.ifBlank { profileName } ?: profileName,
                    email = authenticatedEmail.orEmpty(),
                    profileImage = currentUser.photoUrl ?: currentUser.profile?.profileImage
                )
                    .toEntity()
                    .copy(ownerUid = currentUser.uid)
            )
            return
        }
        val remoteProfile = currentUser.profile
        val updated = existing.copy(
            firstName = profileName.ifBlank { existing.firstName },
            nickname = remoteProfile?.nickname?.takeIf(String::isNotBlank)
                ?: profileName.ifBlank { existing.nickname },
            email = authenticatedEmail ?: existing.email,
            gender = remoteProfile?.gender?.takeIf(String::isNotBlank) ?: existing.gender,
            country = remoteProfile?.country?.takeIf(String::isNotBlank) ?: existing.country,
            region = remoteProfile?.region?.takeIf(String::isNotBlank) ?: existing.region,
            profession = remoteProfile?.profession?.takeIf(String::isNotBlank) ?: existing.profession,
            dressEnvironment = remoteProfile?.dressEnvironment?.takeIf(String::isNotBlank) ?: existing.dressEnvironment,
            stylePreferences = remoteProfile?.stylePreferences?.takeIf(List<String>::isNotEmpty)?.joinToString(",")
                ?: existing.stylePreferences,
            favoriteColors = remoteProfile?.favoriteColors?.takeIf(List<String>::isNotEmpty)?.joinToString(",")
                ?: existing.favoriteColors,
            avoidedColors = remoteProfile?.avoidedColors?.takeIf(List<String>::isNotEmpty)?.joinToString(",")
                ?: existing.avoidedColors,
            commonOccasions = remoteProfile?.commonOccasions?.takeIf(List<String>::isNotEmpty)?.joinToString(",")
                ?: existing.commonOccasions,
            climate = remoteProfile?.climate?.takeIf(String::isNotBlank) ?: existing.climate,
            silhouetteArchetype = remoteProfile?.silhouetteArchetype?.takeIf(String::isNotBlank)
                ?: existing.silhouetteArchetype,
            profileImage = currentUser.photoUrl ?: remoteProfile?.profileImage ?: existing.profileImage
        )
        if (updated != existing) dao.insertOrUpdateProfile(updated)
    }

    private fun remoteName(profile: UserProfile?): String =
        profile?.firstName?.trim()?.takeIf(String::isNotEmpty).orEmpty()
}
