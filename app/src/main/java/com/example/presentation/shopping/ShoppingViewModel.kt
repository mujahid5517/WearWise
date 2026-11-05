package com.example.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ClothingItem
import com.example.domain.model.ShoppingChatMessage
import com.example.domain.model.UserProfile
import com.example.domain.model.WeatherForecast
import com.example.domain.repository.ShoppingRepository
import com.example.domain.usecase.GetWeatherUseCase
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.domain.usecase.UserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val messages: List<ShoppingChatMessage> = listOf(
        ShoppingChatMessage(
            text = "Hello! I'm your WearWise AI Fashion Assistant. Ask me anything about outfit styling, color combinations, wardrobe gaps, or what to wear for any occasion or weather!",
            isUser = false
        )
    ),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedOccasion: String = "Everyday",
    val focusCategory: String? = null,
    val budget: Double = 150.0,
    val currency: String = "USD",
    val wardrobeCount: Int = 0,
    val profileName: String = "",
    val weatherDescription: String? = null,
    val lastFailedMessage: String? = null
)

class ShoppingViewModel(
    private val shoppingRepository: ShoppingRepository? = null,
    private val manageWardrobeUseCase: ManageWardrobeUseCase? = null,
    private val userProfileUseCase: UserProfileUseCase? = null,
    private val getWeatherUseCase: GetWeatherUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private val _wardrobeItems = MutableStateFlow<List<ClothingItem>>(emptyList())
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private val _weatherForecast = MutableStateFlow<WeatherForecast?>(null)

    init {
        loadWardrobeAndProfile()
        loadWeather()
    }

    private fun loadWardrobeAndProfile() {
        val wardrobeFlow = manageWardrobeUseCase?.getAllItems()
        val profileFlow = userProfileUseCase?.getProfile()

        if (wardrobeFlow != null && profileFlow != null) {
            viewModelScope.launch {
                combine(wardrobeFlow, profileFlow) { wardrobe, profile ->
                    _wardrobeItems.value = wardrobe
                    _userProfile.value = profile
                    val name = profile?.nickname?.ifBlank { profile.firstName }?.takeIf { it.isNotBlank() } ?: "User"
                    _uiState.value.copy(
                        wardrobeCount = wardrobe.size,
                        profileName = name
                    )
                }.catch {
                    // Ignore flow errors
                }.collect { newState ->
                    _uiState.value = newState
                }
            }
        }
    }

    private fun loadWeather() {
        viewModelScope.launch {
            getWeatherUseCase?.let { useCase ->
                runCatching { useCase.cached() }
                    .getOrNull()?.let { forecast ->
                        _weatherForecast.value = forecast
                        val desc = forecast.temperatureC?.let { temp ->
                            "⛅ ${temp.toInt()}°C"
                        }
                        _uiState.value = _uiState.value.copy(weatherDescription = desc)
                    }
            }
        }
    }

    fun selectOccasion(occasion: String) {
        _uiState.value = _uiState.value.copy(selectedOccasion = occasion)
    }

    fun selectQuickPrompt(promptText: String) {
        sendMessage(promptText)
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isLoading) return

        val repo = shoppingRepository
        if (repo == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Shopping repository is unavailable."
            )
            return
        }

        val userMsg = ShoppingChatMessage(text = trimmed, isUser = true)
        val currentMessages = _uiState.value.messages + userMsg

        _uiState.value = _uiState.value.copy(
            messages = currentMessages,
            isLoading = true,
            errorMessage = null,
            lastFailedMessage = null
        )

        viewModelScope.launch {
            try {
                val history = if (currentMessages.size > 2) currentMessages.drop(1).dropLast(1) else emptyList()
                val response = repo.chat(
                    question = trimmed,
                    history = history,
                    wardrobe = _wardrobeItems.value,
                    profile = _userProfile.value,
                    budget = _uiState.value.budget,
                    currency = _uiState.value.currency,
                    occasion = _uiState.value.selectedOccasion,
                    weather = _uiState.value.weatherDescription,
                    focusCategory = _uiState.value.focusCategory
                )

                val aiMsg = ShoppingChatMessage(text = response, isUser = false)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiMsg,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to receive response from AI Assistant.",
                    lastFailedMessage = trimmed
                )
            }
        }
    }

    fun retryLastMessage() {
        val failed = _uiState.value.lastFailedMessage ?: return
        sendMessage(failed)
    }

    fun clearChat() {
        _uiState.value = _uiState.value.copy(
            messages = listOf(
                ShoppingChatMessage(
                    text = "Chat history cleared. How can I assist you with your wardrobe or outfit choices now?",
                    isUser = false
                )
            ),
            errorMessage = null,
            lastFailedMessage = null
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
