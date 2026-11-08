package com.example.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ApiKeyManager
import com.example.data.local.AppDatabase
import com.example.data.local.AppLanguage
import com.example.data.local.LanguageManager
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthenticatedUser
import com.example.domain.repository.AuthenticationRepository
import com.example.domain.repository.AuthenticationState
import com.example.domain.repository.ProfileSyncException
import com.example.domain.repository.ProfileSyncStatus
import com.example.domain.repository.ProfileUpdateResult
import com.example.domain.usecase.UserProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileAccountFlowTest {

    @Test
    fun `full name update reaches authentication state and Room`() = withFixture { fixture ->
        val profile = UserProfile(firstName = "Account Owner", nickname = "Account Owner", email = "old@example.com")
        var saved = false

        fixture.viewModel.saveProfile(profile.copy(firstName = "Ada Lovelace"), "") { saved = true }
        awaitCondition { saved }

        assertTrue("Profile update failed: ${fixture.viewModel.accountAction.value.error}", saved)
        assertEquals("Ada Lovelace", fixture.authentication.lastUpdatedProfile?.firstName)
        awaitRoomName(fixture, "Ada Lovelace")
        assertEquals(
            "Ada Lovelace",
            fixture.database.userProfileDao().getUserProfileOnce("account-1")?.firstName
        )
    }

    @Test
    fun `email stays active until verification and then persists for that account`() = withFixture { fixture ->
        val profile = UserProfile(firstName = "Account Owner", nickname = "Account Owner", email = "old@example.com")
        fixture.viewModel.saveProfile(profile.copy(email = "new@example.com"), "CurrentPass123") {}
        awaitCondition { fixture.viewModel.accountAction.value.message?.contains("Verify") == true }

        assertEquals("old@example.com", fixture.authentication.state.value.user?.email)
        assertEquals(
            "old@example.com",
            fixture.database.userProfileDao().getUserProfileOnce("account-1")?.email
        )
        assertTrue(fixture.viewModel.accountAction.value.message.orEmpty().contains("Verify"))

        fixture.authentication.confirmPendingEmail()
        fixture.viewModel.refreshAccount()
        awaitCondition { fixture.viewModel.accountAction.value.message == "Email address updated." }
        awaitRoomEmail(fixture, "new@example.com")

        assertEquals("new@example.com", fixture.authentication.state.value.user?.email)
        assertEquals(
            "new@example.com",
            fixture.database.userProfileDao().getUserProfileOnce("account-1")?.email
        )
    }

    @Test
    fun `password change requires matching confirmation and current password`() = withFixture { fixture ->
        fixture.viewModel.changePassword("CurrentPass123", "NewPass123", "Different123") {}
        assertEquals(0, fixture.authentication.passwordUpdates)
        assertEquals("Passwords do not match.", fixture.viewModel.accountAction.value.error)

        var changed = false
        fixture.viewModel.changePassword("CurrentPass123", "NewPass123", "NewPass123") { changed = true }
        awaitCondition { changed }

        assertTrue(changed)
        assertEquals("CurrentPass123", fixture.authentication.lastCurrentPassword)
        assertEquals("NewPass123", fixture.authentication.lastNewPassword)
        val cursor = fixture.database.openHelper.writableDatabase.query("PRAGMA table_info(user_profiles)")
        val columns = buildSet {
            cursor.use {
                val nameIndex = it.getColumnIndexOrThrow("name")
                while (it.moveToNext()) add(it.getString(nameIndex))
            }
        }
        assertFalse(columns.contains("password"))
    }

    @Test
    fun `navigation labels are readable in English and Spanish`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val languageManager = LanguageManager(context)
        val keys = listOf("nav_outfits", "nav_closet", "nav_avatar", "nav_planner", "nav_shop", "nav_profile")
        val english = listOf("Outfits", "Closet", "Avatar", "Planner", "Shop", "Profile")
        val spanish = listOf("Conjuntos", "Armario", "Avatar", "Planificador", "Tienda", "Perfil")

        assertEquals(english, keys.map { languageManager.getString(it, AppLanguage.ENGLISH) })
        assertEquals(spanish, keys.map { languageManager.getString(it, AppLanguage.SPANISH) })
    }

    @Test
    fun `authentication view model validates registration and signs in through repository`() = withFixture { fixture ->
        val authViewModel = com.example.presentation.authentication.AuthViewModel(fixture.authentication)
        authViewModel.register("Mujahid Mohamed", "mujahid@example.com", "weak", "weak", true) {}
        assertEquals(0, fixture.authentication.registrationCalls)
        assertEquals(
            "Use at least 8 characters with letters and numbers.",
            authViewModel.uiState.value.error
        )

        authViewModel.register(
            "Mujahid Mohamed",
            "mujahid@example.com",
            "StrongPass123",
            "DifferentPass123",
            true
        ) {}
        assertEquals("Passwords do not match.", authViewModel.uiState.value.error)
        assertEquals(0, fixture.authentication.registrationCalls)

        var registered = false
        authViewModel.register(
            "Mujahid Mohamed",
            "mujahid@example.com",
            "StrongPass123",
            "StrongPass123",
            true
        ) { registered = true }
        awaitCondition { registered }
        assertEquals(1, fixture.authentication.registrationCalls)
        assertEquals("Mujahid Mohamed", fixture.authentication.registeredName)
        assertEquals("mujahid@example.com", fixture.authentication.registeredEmail)

        var signedIn = false
        authViewModel.signIn("mujahid@example.com", "StrongPass123") { signedIn = true }
        awaitCondition { signedIn }
        assertEquals(1, fixture.authentication.signInCalls)
    }

    @Test
    fun `profile sync retry does not repeat authentication`() = withFixture { fixture ->
        val authViewModel = com.example.presentation.authentication.AuthViewModel(fixture.authentication)
        fixture.authentication.signInSyncError = ProfileSyncException(
            "Firestore denied profile access.",
            IllegalStateException("PERMISSION_DENIED")
        )
        authViewModel.signIn("mujahid@example.com", "StrongPass123") {}
        awaitCondition { authViewModel.uiState.value.error != null }

        assertEquals(1, fixture.authentication.signInCalls)
        assertEquals("account-1", fixture.authentication.state.value.user?.uid)
        assertEquals(ProfileSyncStatus.FAILED, fixture.authentication.state.value.profileSyncStatus)
        assertEquals("Firestore denied profile access.", authViewModel.uiState.value.error)

        fixture.authentication.signInSyncError = null
        var completed = false
        authViewModel.retryProfileSync { completed = true }
        awaitCondition { completed }

        assertTrue(completed)
        assertEquals(1, fixture.authentication.signInCalls)
        assertEquals(1, fixture.authentication.profileSyncRetries)
        assertEquals(ProfileSyncStatus.SYNCED, fixture.authentication.state.value.profileSyncStatus)
    }

    private fun withFixture(test: suspend (Fixture) -> Unit) = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val authentication = FakeAuthenticationRepository()
        val store = ViewModelStore()
        val viewModel = ProfileViewModel(
            UserProfileUseCase(UserRepositoryImpl(database, authentication)),
            ApiKeyManager(context),
            authentication,
            database,
            context
        )
        store.put("profile", viewModel)
        val fixture = Fixture(database, authentication, viewModel)

        try {
            test(fixture)
        } finally {
            store.clear()
            database.close()
            Dispatchers.resetMain()
        }
    }

    private suspend fun awaitCondition(condition: () -> Boolean) {
        withTimeout(5_000) {
            while (!condition()) delay(10)
        }
    }

    private suspend fun awaitRoomEmail(fixture: Fixture, email: String) {
        withTimeout(5_000) {
            while (fixture.database.userProfileDao().getUserProfileOnce("account-1")?.email != email) {
                delay(10)
            }
        }
    }

    private suspend fun awaitRoomName(fixture: Fixture, name: String) {
        withTimeout(5_000) {
            while (fixture.database.userProfileDao().getUserProfileOnce("account-1")?.firstName != name) {
                delay(10)
            }
        }
    }

    private data class Fixture(
        val database: AppDatabase,
        val authentication: FakeAuthenticationRepository,
        val viewModel: ProfileViewModel
    )

    private class FakeAuthenticationRepository : AuthenticationRepository {
        private val signedInUser = AuthenticatedUser(
            uid = "account-1",
            displayName = "Account Owner",
            email = "old@example.com",
            photoUrl = null,
            emailVerified = true
        )
        private var serverUser = signedInUser
        private var pendingEmail: String? = null
        private val mutableState = MutableStateFlow(
            AuthenticationState(isReady = true, user = signedInUser)
        )
        override val state: StateFlow<AuthenticationState> = mutableState
        var passwordUpdates = 0
        var lastCurrentPassword: String? = null
        var lastNewPassword: String? = null
        var lastUpdatedProfile: UserProfile? = null
        var registrationCalls = 0
        var signInCalls = 0
        var registeredName: String? = null
        var registeredEmail: String? = null
        var signInSyncError: ProfileSyncException? = null
        var profileSyncRetries = 0

        override suspend fun register(fullName: String, email: String, password: String): AuthenticatedUser {
            registrationCalls++
            registeredName = fullName
            registeredEmail = email
            serverUser = signedInUser.copy(displayName = fullName, email = email)
            mutableState.value = AuthenticationState(isReady = true, user = serverUser)
            return serverUser
        }

        override suspend fun signIn(email: String, password: String): AuthenticatedUser {
            signInCalls++
            serverUser = signedInUser.copy(email = email)
            signInSyncError?.let {
                mutableState.value = AuthenticationState(
                    isReady = true,
                    user = serverUser,
                    profileSyncStatus = ProfileSyncStatus.FAILED,
                    profileSyncError = it.message
                )
                throw it
            }
            mutableState.value = AuthenticationState(
                isReady = true,
                user = serverUser,
                profileSyncStatus = ProfileSyncStatus.SYNCED
            )
            return serverUser
        }
        override suspend fun syncCurrentUserProfile(): AuthenticatedUser {
            profileSyncRetries++
            mutableState.value = AuthenticationState(
                isReady = true,
                user = serverUser,
                profileSyncStatus = ProfileSyncStatus.SYNCED
            )
            return serverUser
        }
        override suspend fun refreshCurrentUser(): AuthenticatedUser {
            mutableState.value = AuthenticationState(isReady = true, user = serverUser)
            return serverUser
        }
        override suspend fun sendPasswordReset(email: String) = error("Unused")
        override suspend fun updatePassword(currentPassword: String, newPassword: String) {
            passwordUpdates++
            lastCurrentPassword = currentPassword
            lastNewPassword = newPassword
        }
        override suspend fun updateProfile(
            profile: UserProfile,
            currentPassword: String?
        ): ProfileUpdateResult {
            lastUpdatedProfile = profile
            serverUser = serverUser.copy(displayName = profile.firstName)
            pendingEmail = profile.email.takeIf { !it.equals(serverUser.email, ignoreCase = true) }
            mutableState.value = AuthenticationState(isReady = true, user = serverUser)
            return ProfileUpdateResult(pendingEmail)
        }
        override suspend fun updateProfilePhoto(localUri: String) = localUri
        override suspend fun deleteAccount(password: String) = Unit
        override fun signOut() {
            mutableState.value = AuthenticationState(isReady = true)
        }

        fun confirmPendingEmail() {
            val email = requireNotNull(pendingEmail)
            serverUser = serverUser.copy(email = email, emailVerified = true)
        }
    }
}
