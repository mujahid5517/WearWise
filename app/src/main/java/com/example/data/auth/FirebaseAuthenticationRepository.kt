package com.example.data.auth

import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.AuthenticationRepository
import com.example.domain.repository.AuthenticationState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthenticationRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: com.google.firebase.storage.FirebaseStorage? = null
) : AuthenticationRepository {

    private val _state = MutableStateFlow(
        AuthenticationState(
            isReady = true,
            user = auth.currentUser?.let {
                AuthenticatedUser(it.uid, it.displayName, it.email, it.photoUrl?.toString(), it.isEmailVerified)
            }
        )
    )
    override val state: StateFlow<AuthenticationState> = _state.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            _state.value = AuthenticationState(
                isReady = true,
                user = firebaseUser?.let {
                    AuthenticatedUser(it.uid, it.displayName, it.email, it.photoUrl?.toString(), it.isEmailVerified)
                }
            )
        }
    }

    override suspend fun register(fullName: String, email: String, password: String): AuthenticatedUser {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: error("Registration failed")
        return AuthenticatedUser(user.uid, fullName, user.email, null, user.isEmailVerified)
    }

    override suspend fun signIn(email: String, password: String): AuthenticatedUser {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: error("Sign in failed")
        return AuthenticatedUser(user.uid, user.displayName, user.email, null, user.isEmailVerified)
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String) {
        auth.currentUser?.updatePassword(newPassword)?.await()
    }

    override suspend fun updateProfile(profile: UserProfile) {}

    override suspend fun updateProfilePhoto(localUri: String): String = localUri

    override suspend fun deleteAccount(password: String) {
        auth.currentUser?.delete()?.await()
    }

    override fun signOut() {
        auth.signOut()
    }
}
