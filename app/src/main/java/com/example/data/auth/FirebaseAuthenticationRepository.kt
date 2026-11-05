package com.example.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.domain.repository.AuthenticationRepository
import com.example.domain.repository.AuthenticationState
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.UserRepository
import com.example.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow

class FirebaseAuthenticationRepository(
    private val auth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?,
    private val storage: FirebaseStorage?
) : AuthenticationRepository, UserRepository {
    override val state: StateFlow<AuthenticationState> = MutableStateFlow(AuthenticationState(isReady = true))

    override suspend fun register(fullName: String, email: String, password: String): AuthenticatedUser {
        return AuthenticatedUser("", fullName, email, null, true)
    }

    override suspend fun signIn(email: String, password: String): AuthenticatedUser {
        return AuthenticatedUser("", "User", email, null, true)
    }

    override suspend fun sendPasswordReset(email: String) {}

    override suspend fun updatePassword(currentPassword: String, newPassword: String) {}

    override suspend fun updateProfile(profile: UserProfile) {}

    override suspend fun updateProfilePhoto(localUri: String): String = localUri

    override suspend fun deleteAccount(password: String) {}

    override fun signOut() {}

    override fun getUserProfile(): Flow<UserProfile?> = MutableStateFlow(UserProfile())

    override suspend fun saveUserProfile(profile: UserProfile) {}

    override suspend fun resetLearnedPreferences() {}
}
