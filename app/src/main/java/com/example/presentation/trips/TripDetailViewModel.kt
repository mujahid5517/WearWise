package com.example.presentation.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Trip
import com.example.domain.model.TripPackingItem
import com.example.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripDetailUiState(
    val trip: Trip? = null,
    val isLoading: Boolean = true,
    val isCheckingWeather: Boolean = false,
    val errorMessage: String? = null
)

class TripDetailViewModel(
    private val tripRepository: TripRepository,
    private val tripId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    init {
        loadTrip()
    }

    fun loadTrip() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val trip = tripRepository.getTripById(tripId)
                _uiState.update { it.copy(trip = trip, isLoading = false) }
                // Proactively check for weather shifts / re-packing alerts
                checkWeatherShiftAlert()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Failed to load trip.") }
            }
        }
    }

    fun checkWeatherShiftAlert() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingWeather = true) }
            try {
                tripRepository.checkRepackingAlert(tripId)
                val refreshed = tripRepository.getTripById(tripId)
                _uiState.update { it.copy(trip = refreshed, isCheckingWeather = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isCheckingWeather = false) }
            }
        }
    }

    fun autoApplyRepackingRecommendations() {
        viewModelScope.launch {
            try {
                val updated = tripRepository.autoApplyRepackingRecommendations(tripId)
                if (updated != null) {
                    _uiState.update { it.copy(trip = updated) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Could not apply repacking items: ${e.localizedMessage}") }
            }
        }
    }

    fun dismissRepackingAlert() {
        val currentTrip = _uiState.value.trip ?: return
        val updated = currentTrip.copy(repackingAlert = "")
        _uiState.update { it.copy(trip = updated) }
        viewModelScope.launch {
            tripRepository.updateTrip(updated)
        }
    }

    fun togglePackingItem(itemId: String) {
        val currentTrip = _uiState.value.trip ?: return
        val updatedList = currentTrip.packingList.map { item ->
            if (item.id == itemId) item.copy(isPacked = !item.isPacked) else item
        }
        val updatedTrip = currentTrip.copy(packingList = updatedList)
        _uiState.update { it.copy(trip = updatedTrip) }

        viewModelScope.launch {
            try {
                tripRepository.updateTrip(updatedTrip)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update packing checklist: ${e.localizedMessage}") }
            }
        }
    }

    fun addCustomPackingItem(category: String, itemName: String) {
        val currentTrip = _uiState.value.trip ?: return
        if (itemName.trim().isBlank()) return

        val newItem = TripPackingItem(
            category = category.trim(),
            itemName = itemName.trim(),
            isPacked = false,
            clothingItemId = null
        )
        val updatedTrip = currentTrip.copy(packingList = currentTrip.packingList + newItem)
        _uiState.update { it.copy(trip = updatedTrip) }

        viewModelScope.launch {
            try {
                tripRepository.updateTrip(updatedTrip)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to add item: ${e.localizedMessage}") }
            }
        }
    }

    fun deletePackingItem(itemId: String) {
        val currentTrip = _uiState.value.trip ?: return
        val updatedTrip = currentTrip.copy(packingList = currentTrip.packingList.filterNot { it.id == itemId })
        _uiState.update { it.copy(trip = updatedTrip) }

        viewModelScope.launch {
            try {
                tripRepository.updateTrip(updatedTrip)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete item: ${e.localizedMessage}") }
            }
        }
    }

    fun setAllItemsPacked(packed: Boolean) {
        val currentTrip = _uiState.value.trip ?: return
        val updatedList = currentTrip.packingList.map { it.copy(isPacked = packed) }
        val updatedTrip = currentTrip.copy(packingList = updatedList)
        _uiState.update { it.copy(trip = updatedTrip) }

        viewModelScope.launch {
            try {
                tripRepository.updateTrip(updatedTrip)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update items: ${e.localizedMessage}") }
            }
        }
    }

    fun deleteTrip(onDeleted: () -> Unit) {
        val currentTrip = _uiState.value.trip ?: return
        viewModelScope.launch {
            try {
                tripRepository.deleteTrip(currentTrip.id)
                onDeleted()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete trip: ${e.localizedMessage}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
