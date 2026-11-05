package com.example.presentation.authentication

import androidx.lifecycle.ViewModel
import com.example.domain.repository.AuthenticationRepository

class AuthViewModel(
    val authenticationRepository: AuthenticationRepository? = null
) : ViewModel()

