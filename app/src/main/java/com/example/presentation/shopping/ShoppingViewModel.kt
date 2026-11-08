package com.example.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ClothingItem
import com.example.domain.model.ShoppingChatMessage
import com.example.domain.model.ShoppingItem
import com.example.domain.model.ShoppingAnalysis
import com.example.domain.model.ShoppingBudget
import com.example.domain.model.UserProfile
import com.example.domain.model.WeatherForecast
import com.example.domain.repository.OutfitFeedbackRepository
import com.example.domain.repository.ShoppingRepository
import com.example.domain.usecase.GetWeatherUseCase
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.domain.usecase.UserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val messages: List<ShoppingChatMessage> = listOf(
        ShoppingChatMessage(
            text = "Hello! I can give local styling and wardrobe-gap suggestions from your saved closet, profile, weather, budget, and outfit feedback. No live store catalog or purchase links are connected.",
            isUser = false
        )
    ),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedOccasion: String = "Everyday",
    val focusCategory: String? = null,
    val budget: Double = 0.0,
    val currency: String = "USD",
    val wardrobeCount: Int = 0,
    val profileName: String = "",
    val weatherDescription: String? = null,
    val lastFailedMessage: String? = null,
    val analysis: ShoppingAnalysis? = null,
    val syncMessage: String? = null,
    val isSavingBudget: Boolean = false
)

class ShoppingViewModel(
    private val shoppingRepository: ShoppingRepository? = null,
    private val manageWardrobeUseCase: ManageWardrobeUseCase? = null,
    private val userProfileUseCase: UserProfileUseCase? = null,
    private val getWeatherUseCase: GetWeatherUseCase? = null,
    private val feedbackRepository: OutfitFeedbackRepository? = null
) : ViewModel() {

    val shoppingItems: StateFlow<List<ShoppingItem>> = shoppingRepository
        ?.observeShoppingItems()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val purchaseHistory: StateFlow<List<ShoppingItem>> = shoppingRepository
        ?.observePurchaseHistory()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private val _wardrobeItems = MutableStateFlow<List<ClothingItem>>(emptyList())
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private val _weatherForecast = MutableStateFlow<WeatherForecast?>(null)

    init {
        shoppingRepository?.let { repository ->
            viewModelScope.launch {
                repository.observeBudget().collect { budget ->
                    _uiState.value = _uiState.value.copy(budget = budget.amount, currency = budget.currency)
                    refreshRecommendations()
                }
            }
            viewModelScope.launch {
                repository.syncMessage.collect { message ->
                    _uiState.value = _uiState.value.copy(syncMessage = message)
                }
            }
        }
        loadWardrobeAndProfile()
        loadWeather()
        feedbackRepository?.let { repository ->
            viewModelScope.launch { repository.observeFeedback().collect { refreshRecommendations() } }
        }
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
                    emit(_uiState.value)
                }.collect { newState ->
                    _uiState.value = newState
                    refreshRecommendations()
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
                            val condition = forecast.weatherCode?.let(com.example.domain.model.WeatherDescriptions::describe)
                            val rain = forecast.precipitationProbability?.let { " · rain $it%" }.orEmpty()
                            "⛅ ${temp.toInt()}°C${condition?.let { " · $it" }.orEmpty()}$rain"
                        }
                        _uiState.value = _uiState.value.copy(weatherDescription = desc)
                        refreshRecommendations()
                    }
            }
        }
    }

    fun selectOccasion(occasion: String) {
        _uiState.value = _uiState.value.copy(selectedOccasion = occasion)
        refreshRecommendations()
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
                    text = "Chat history cleared. Ask for local styling or wardrobe-gap suggestions from your saved data.",
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

    fun addItem(item: ShoppingItem) {
        shoppingRepository?.let { repository ->
            viewModelScope.launch { repository.addItem(item) }
        }
    }

    fun setFavorite(id: Long, favorite: Boolean) {
        shoppingRepository?.let { repository ->
            viewModelScope.launch { repository.setFavorite(id, favorite) }
        }
    }

    fun markPurchased(item: ShoppingItem, actualPrice: Double) {
        updateItem(
            item.copy(
                status = "PURCHASED",
                actualPrice = actualPrice.coerceAtLeast(0.0),
                purchasedAt = item.purchasedAt ?: System.currentTimeMillis()
            )
        )
    }

    fun markNeeded(item: ShoppingItem) {
        updateItem(item.copy(status = "NEEDED", purchasedAt = null, actualPrice = null))
    }

    fun saveBudget(amount: Double, currency: String = _uiState.value.currency) {
        val repository = shoppingRepository ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingBudget = true)
            runCatching { repository.updateBudget(ShoppingBudget(amount = amount, currency = currency)) }
            _uiState.value = _uiState.value.copy(isSavingBudget = false)
        }
    }

    fun refreshRecommendations() {
        val repository = shoppingRepository ?: return
        viewModelScope.launch {
            val state = _uiState.value
            val feedback = runCatching { feedbackRepository?.preferenceSummary().orEmpty() }.getOrDefault(emptyMap())
            val analysis = runCatching {
                repository.analyzeWardrobe(
                    wardrobe = _wardrobeItems.value,
                    profile = _userProfile.value,
                    budget = state.budget,
                    currency = state.currency,
                    occasion = state.selectedOccasion,
                    weather = state.weatherDescription ?: _weatherForecast.value?.temperatureC?.let { "${it.toInt()}°C" },
                    focusCategory = state.focusCategory,
                    feedback = feedback
                )
            }.getOrNull()
            _uiState.value = _uiState.value.copy(analysis = analysis)
        }
    }

    fun updateItem(item: ShoppingItem) {
        shoppingRepository?.let { repository ->
            viewModelScope.launch { repository.updateItem(item) }
        }
    }

    fun deleteItem(id: Long) {
        shoppingRepository?.let { repository ->
            viewModelScope.launch { repository.deleteItem(id) }
        }
    }
}
