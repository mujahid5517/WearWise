package com.example.data.auth

import android.net.Uri
import android.util.Log
import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.AuthenticationRepository
import com.example.domain.repository.AuthenticationState
import com.example.domain.repository.ProfileSyncException
import com.example.domain.repository.ProfileSyncStatus
import com.example.domain.repository.ProfileUpdateResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthenticationRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage? = null
) : AuthenticationRepository {

    private val _state = MutableStateFlow(
        AuthenticationState(
            isReady = true,
            user = auth.currentUser?.toAuthenticatedUser(),
            profileSyncStatus = if (auth.currentUser == null) {
                ProfileSyncStatus.NOT_REQUIRED
            } else {
                ProfileSyncStatus.SYNCING
            }
        )
    )
    override val state: StateFlow<AuthenticationState> = _state.asStateFlow()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var authOperationInProgress = false

    init {
        auth.addAuthStateListener { firebaseAuth ->
            if (authOperationInProgress) return@addAuthStateListener
            val user = firebaseAuth.currentUser
            if (user == null) {
                publishUser(null)
            } else {
                publishUser(user, profileSyncStatus = ProfileSyncStatus.SYNCING)
                repositoryScope.launch {
                    try {
                        val profile = ensureUserDocument(user)
                        publishUser(user, profile)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        publishProfileSyncFailure(user, error)
                    }
                }
            }
        }
    }

    override suspend fun register(fullName: String, email: String, password: String): AuthenticatedUser {
        authOperationInProgress = true
        try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: error("Registration failed.")
            try {
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(fullName.trim()).build()
                ).await()
                val profile = ensureUserDocument(user, preferredName = fullName.trim())
                publishUser(auth.currentUser ?: user, profile)
                return profile
            } catch (cancelled: CancellationException) {
                publishProfileSyncFailure(user, cancelled)
                throw cancelled
            } catch (error: Exception) {
                val syncError = profileSyncException(error, "create")
                publishUser(
                    auth.currentUser ?: user,
                    profileSyncStatus = ProfileSyncStatus.FAILED,
                    profileSyncError = syncError.message
                )
                throw syncError
            }
        } finally {
            authOperationInProgress = false
        }
    }

    override suspend fun signIn(email: String, password: String): AuthenticatedUser {
        authOperationInProgress = true
        try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: error("Sign in failed.")
            try {
                val profile = ensureUserDocument(user)
                publishUser(auth.currentUser ?: user, profile)
                return profile
            } catch (cancelled: CancellationException) {
                publishProfileSyncFailure(user, cancelled)
                throw cancelled
            } catch (error: Exception) {
                val syncError = profileSyncException(error, "sync")
                publishUser(
                    auth.currentUser ?: user,
                    profileSyncStatus = ProfileSyncStatus.FAILED,
                    profileSyncError = syncError.message
                )
                throw syncError
            }
        } finally {
            authOperationInProgress = false
        }
    }

    override suspend fun refreshCurrentUser(): AuthenticatedUser? {
        val user = auth.currentUser ?: return null
        try {
            user.reload().await()
            val refreshedUser = auth.currentUser ?: user
            val authenticatedUser = ensureUserDocument(refreshedUser)
            publishUser(refreshedUser, authenticatedUser)
            return authenticatedUser
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val syncError = profileSyncException(error, "sync")
            publishUser(
                auth.currentUser ?: user,
                profileSyncStatus = ProfileSyncStatus.FAILED,
                profileSyncError = syncError.message
            )
            throw syncError
        }
    }

    override suspend fun syncCurrentUserProfile(): AuthenticatedUser? {
        val user = auth.currentUser ?: return null
        try {
            val profile = ensureUserDocument(user)
            publishUser(user, profile)
            return profile
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val syncError = profileSyncException(error, "sync")
            publishUser(
                auth.currentUser ?: user,
                profileSyncStatus = ProfileSyncStatus.FAILED,
                profileSyncError = syncError.message
            )
            throw syncError
        }
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser ?: error("Sign in before changing your password.")
        reauthenticate(user, currentPassword)
        user.updatePassword(newPassword).await()
    }

    override suspend fun updateProfile(profile: UserProfile, currentPassword: String?): ProfileUpdateResult {
        val user = auth.currentUser ?: error("Sign in before updating your profile.")
        val fullName = profile.firstName.trim()
        require(fullName.isNotEmpty()) { "Full name cannot be empty." }

        var pendingEmail: String? = null
        val requestedEmail = profile.email.trim()
        if (!requestedEmail.equals(user.email.orEmpty(), ignoreCase = true)) {
            val password = currentPassword?.takeIf(String::isNotBlank)
                ?: throw IllegalArgumentException("Enter your current password to change your email.")
            reauthenticate(user, password)
            user.verifyBeforeUpdateEmail(requestedEmail).await()
            pendingEmail = requestedEmail
        }

        val updateRequest = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName)
            .build()
        user.updateProfile(updateRequest).await()
        val activeUser = auth.currentUser ?: user
        publishUser(activeUser)

        val values = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "fullName" to fullName,
            "displayName" to fullName,
            "nickname" to profile.nickname,
            "email" to (user.email ?: requestedEmail),
            "emailVerified" to user.isEmailVerified,
            "pendingEmail" to (pendingEmail?.let { it } ?: FieldValue.delete()),
            "ageRange" to profile.ageRange,
            "gender" to profile.gender,
            "country" to profile.country,
            "region" to profile.region,
            "profession" to profile.profession,
            "dressEnvironment" to profile.dressEnvironment,
            "stylePreferences" to profile.stylePreferences,
            "favoriteColors" to profile.favoriteColors,
            "avoidedColors" to profile.avoidedColors,
            "commonOccasions" to profile.commonOccasions,
            "climate" to profile.climate,
            "silhouetteArchetype" to profile.silhouetteArchetype,
            "colorSeason" to profile.colorSeason,
            "skinUndertone" to profile.skinUndertone,
            "hairColorAnalyzed" to profile.hairColorAnalyzed,
            "eyeColorAnalyzed" to profile.eyeColorAnalyzed,
            "recommendedColors" to profile.recommendedColors,
            "colorsToAvoid" to profile.colorsToAvoid,
            "colorAnalysisRationale" to profile.colorAnalysisRationale,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        profile.profileImage?.let { values["profileImageUrl"] = it }
        firestore.collection("users")
            .document(user.uid)
            .set(values, SetOptions.merge())
            .await()

        publishUser(
            activeUser,
            activeUser.toAuthenticatedUser().copy(
                profile = profile.copy(email = activeUser.email.orEmpty())
            )
        )
        return ProfileUpdateResult(pendingEmail = pendingEmail)
    }

    override suspend fun updateProfilePhoto(localUri: String): String {
        val user = auth.currentUser ?: error("Sign in before updating your profile photo.")
        val configuredStorage = storage
            ?: error("Profile photo storage is not configured for this Firebase project.")
        val uri = Uri.parse(localUri)
        require(uri.scheme == "content" || uri.scheme == "file") { "Select a valid profile photo." }

        val storagePath = "users/${user.uid}/profile/avatar.jpg"
        val storageReference = configuredStorage.reference.child(storagePath)
        storageReference.putFile(uri).await()
        val downloadUrl = storageReference.downloadUrl.await().toString()
        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setPhotoUri(Uri.parse(downloadUrl))
                .build()
        ).await()

        val activeUser = auth.currentUser ?: user
        publishUser(activeUser)
        firestore.collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "uid" to user.uid,
                    "profileImageUrl" to downloadUrl,
                    "profileImageStoragePath" to storagePath,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
        val currentProfile = state.value.user?.profile ?: UserProfile(
            firstName = activeUser.displayName.orEmpty(),
            email = activeUser.email.orEmpty()
        )
        publishUser(
            activeUser,
            activeUser.toAuthenticatedUser().copy(profile = currentProfile.copy(profileImage = downloadUrl))
        )
        return downloadUrl
    }

    private suspend fun ensureUserDocument(
        user: FirebaseUser,
        preferredName: String? = null
    ): AuthenticatedUser {
        val document = firestore.collection("users").document(user.uid)
        val snapshot = try {
            document.get().await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw profileSyncException(error, "read")
        }
        return try {
            val name = if (snapshot.exists()) {
                snapshot.getString("fullName")?.trim()?.takeIf(String::isNotEmpty)
                    ?: snapshot.getString("displayName")?.trim()?.takeIf(String::isNotEmpty)
                    ?: user.displayName?.trim()?.takeIf(String::isNotEmpty)
                    ?: user.email?.substringBefore('@').orEmpty()
            } else {
                preferredName?.trim()?.takeIf(String::isNotEmpty)
                    ?: user.displayName?.trim()?.takeIf(String::isNotEmpty)
                    ?: user.email?.substringBefore('@').orEmpty()
            }
            val photoUrl = if (snapshot.exists()) {
                snapshot.getString("profileImageUrl") ?: user.photoUrl?.toString()
            } else {
                user.photoUrl?.toString()
            }

            if (!snapshot.exists()) {
                val values = mutableMapOf<String, Any>(
                    "uid" to user.uid,
                    "fullName" to name,
                    "displayName" to name,
                    "email" to user.email.orEmpty(),
                    "emailVerified" to user.isEmailVerified,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (photoUrl != null) values["profileImageUrl"] = photoUrl
                document.set(values, SetOptions.merge()).await()
            }

            val profile = UserProfile(
                firstName = name,
                nickname = snapshot.getString("nickname")?.trim().orEmpty().ifBlank { name },
                email = user.email.orEmpty(),
                ageRange = snapshot.getString("ageRange").orEmpty(),
                gender = snapshot.getString("gender").orEmpty(),
                country = snapshot.getString("country").orEmpty(),
                region = snapshot.getString("region").orEmpty(),
                profession = snapshot.getString("profession").orEmpty(),
                dressEnvironment = snapshot.getString("dressEnvironment").orEmpty(),
                stylePreferences = snapshot.get("stylePreferences").asStringList(),
                favoriteColors = snapshot.get("favoriteColors").asStringList(),
                avoidedColors = snapshot.get("avoidedColors").asStringList(),
                commonOccasions = snapshot.get("commonOccasions").asStringList(),
                climate = snapshot.getString("climate").orEmpty(),
                silhouetteArchetype = snapshot.getString("silhouetteArchetype").orEmpty(),
                profileImage = photoUrl,
                profileImagePath = snapshot.getString("profileImageStoragePath"),
                colorSeason = snapshot.getString("colorSeason").orEmpty(),
                skinUndertone = snapshot.getString("skinUndertone").orEmpty(),
                hairColorAnalyzed = snapshot.getString("hairColorAnalyzed").orEmpty(),
                eyeColorAnalyzed = snapshot.getString("eyeColorAnalyzed").orEmpty(),
                recommendedColors = snapshot.get("recommendedColors").asStringList(),
                colorsToAvoid = snapshot.get("colorsToAvoid").asStringList(),
                colorAnalysisRationale = snapshot.getString("colorAnalysisRationale").orEmpty(),
                colorAnalysisPhotoPath = snapshot.getString("colorAnalysisPhotoPath"),
                createdAt = snapshot.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                updatedAt = snapshot.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
            )
            AuthenticatedUser(
                uid = user.uid,
                displayName = name,
                email = user.email,
                photoUrl = photoUrl,
                emailVerified = user.isEmailVerified,
                profile = profile
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw profileSyncException(error, if (snapshot.exists()) "read" else "create")
        }
    }

    private fun publishProfileSyncFailure(user: FirebaseUser, error: Exception) {
        val syncError = profileSyncException(error, "sync")
        publishUser(
            user,
            profileSyncStatus = ProfileSyncStatus.FAILED,
            profileSyncError = syncError.message
        )
    }

    private fun profileSyncException(error: Exception, operation: String): ProfileSyncException {
        if (error is ProfileSyncException) return error

        val firestoreError = error as? FirebaseFirestoreException
        val code = firestoreError?.code?.name
            ?: (error as? FirebaseAuthException)?.errorCode
            ?: if (error is FirebaseNetworkException) "NETWORK_ERROR" else error.javaClass.simpleName
        Log.e("WearWiseAuth", "User profile $operation failed (code=$code): ${error.message.orEmpty()}")
        val message = when {
            error is FirebaseNetworkException ->
                "Firebase could not be reached while synchronizing your profile. Retry profile synchronization."
            firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Authentication succeeded, but Firestore denied profile access. Check the users/{uid} security rule."
            firestoreError?.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Authentication succeeded, but Firestore rejected the session. Retry profile synchronization."
            firestoreError?.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                firestoreError?.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Firestore is temporarily unavailable. Retry profile synchronization."
            error is IllegalArgumentException || firestoreError?.code == FirebaseFirestoreException.Code.INVALID_ARGUMENT ||
                firestoreError?.code == FirebaseFirestoreException.Code.DATA_LOSS ->
                "The stored profile has an invalid format. Contact support before changing profile data."
            else ->
                "Authentication succeeded, but profile synchronization failed. Retry profile synchronization."
        }
        return ProfileSyncException(message, error)
    }

    private fun Any?.asStringList(): List<String> =
        (this as? List<*>)?.mapNotNull { it as? String }.orEmpty()

    private suspend fun reauthenticate(user: FirebaseUser, password: String) {
        if (user.providerData.none { it.providerId == EmailAuthProvider.PROVIDER_ID }) {
            throw UnsupportedOperationException(
                "This account's sign-in provider does not support password-based account changes."
            )
        }
        val email = user.email
            ?: throw UnsupportedOperationException("This account does not have an email/password credential.")
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
    }

    private fun publishUser(
        user: FirebaseUser?,
        profile: AuthenticatedUser? = null,
        profileSyncStatus: ProfileSyncStatus = if (user == null) {
            ProfileSyncStatus.NOT_REQUIRED
        } else {
            ProfileSyncStatus.SYNCED
        },
        profileSyncError: String? = null
    ) {
        _state.value = AuthenticationState(
            isReady = true,
            user = user?.let {
                profile?.takeIf { authenticated -> authenticated.uid == it.uid } ?: it.toAuthenticatedUser()
            },
            profileSyncStatus = profileSyncStatus,
            profileSyncError = profileSyncError
        )
    }

    private fun FirebaseUser.toAuthenticatedUser() = AuthenticatedUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString(),
        emailVerified = isEmailVerified
    )

    override suspend fun deleteAccount(password: String) {
        val user = auth.currentUser ?: error("Sign in before deleting your account.")
        reauthenticate(user, password)

        val profileDocument = firestore.collection("users").document(user.uid)
        val profile = profileDocument.get().await()
        val profileStoragePath = profile.getString("profileImageStoragePath")
        if (!profileStoragePath.isNullOrBlank()) {
            storage?.reference?.child(profileStoragePath)?.delete()?.await()
        }
        profileDocument.delete().await()
        user.delete().await()
        publishUser(null)
    }

    override fun signOut() {
        auth.signOut()
        publishUser(null)
    }
}
