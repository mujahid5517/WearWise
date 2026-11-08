package com.example.presentation.authentication

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.AuthenticationRepository
import com.example.domain.repository.ProfileSyncException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class AuthViewModel(
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    val authenticationState = authRepository.state

    fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean,
        onSuccess: () -> Unit
    ) {
        val name = fullName.trim()
        val normalizedEmail = email.trim()
        when {
            name.isEmpty() -> return showError("Enter your full name.")
            !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() ->
                return showError("Enter a valid email address.")
            !isStrongPassword(password) ->
                return showError("Use at least 8 characters with letters and numbers.")
            password != confirmPassword -> return showError("Passwords do not match.")
            !acceptedTerms -> return showError("Accept the Terms and Privacy Policy to continue.")
        }

        runAuthAction(onSuccess, signIn = false) {
            authRepository.register(name, normalizedEmail, password)
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        val normalizedEmail = email.trim()
        when {
            !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() ->
                return showError("Enter a valid email address.")
            password.isBlank() -> return showError("Enter your password.")
        }
        runAuthAction(onSuccess, signIn = true) {
            authRepository.signIn(normalizedEmail, password)
        }
    }

    fun sendPasswordReset(email: String) {
        val normalizedEmail = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            showError("Enter a valid email address first.")
            return
        }
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                authRepository.sendPasswordReset(normalizedEmail)
                _uiState.value = AuthUiState(message = "Password reset instructions were sent.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = AuthUiState(error = friendlyAuthError(error, signIn = false))
            }
        }
    }

    fun retryProfileSync(onSuccess: () -> Unit) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                checkNotNull(authRepository.syncCurrentUserProfile()) {
                    "Sign in before retrying profile synchronization."
                }
                _uiState.value = AuthUiState()
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = AuthUiState(error = friendlyAuthError(error, signIn = true))
            }
        }
    }

    fun clearFeedback() {
        if (!_uiState.value.isLoading) _uiState.value = AuthUiState()
    }

    private fun runAuthAction(
        onSuccess: () -> Unit,
        signIn: Boolean,
        action: suspend () -> Unit
    ) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                action()
                _uiState.value = AuthUiState()
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = AuthUiState(error = friendlyAuthError(error, signIn))
            }
        }
    }

    private fun showError(message: String) {
        if (!_uiState.value.isLoading) _uiState.value = AuthUiState(error = message)
    }

    private fun friendlyAuthError(error: Exception, signIn: Boolean): String = when {
        error is ProfileSyncException -> error.message
            ?: "Authentication succeeded, but profile synchronization failed. Retry profile synchronization."
        error is FirebaseAuthUserCollisionException ->
            "An account already uses this email address. Try signing in."
        error is FirebaseAuthWeakPasswordException ->
            "Choose a stronger password with at least 8 characters, letters, and numbers."
        error is FirebaseNetworkException ->
            "Network unavailable. Check your connection and try again."
        error is FirebaseAuthInvalidCredentialsException ->
            if (signIn) "Email or password is incorrect." else "Check your email address and try again."
        error is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Firestore denied profile access. Check the users/{uid} security rule."
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Firestore is temporarily unavailable. Try again."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Firestore rejected the authenticated session. Try again."
            else -> "Profile synchronization failed (${error.code}). Try again."
        }
        error is FirebaseAuthException && error.errorCode == "ERROR_EMAIL_ALREADY_IN_USE" ->
            "An account already uses this email address. Try signing in."
        error is FirebaseAuthException && error.errorCode == "ERROR_INVALID_EMAIL" ->
            "Enter a valid email address."
        error is FirebaseAuthException &&
            (error.errorCode == "ERROR_WRONG_PASSWORD" ||
                error.errorCode == "ERROR_INVALID_CREDENTIAL" ||
                error.errorCode == "ERROR_USER_NOT_FOUND") ->
            "Email or password is incorrect."
        else -> "We couldn't complete that request. Please try again."
    }

    private fun isStrongPassword(password: String): Boolean =
        password.length >= 8 && password.any(Char::isLetter) && password.any(Char::isDigit)
}
