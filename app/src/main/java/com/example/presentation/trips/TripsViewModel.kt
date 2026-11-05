package com.example.presentation.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ClothingItem
import com.example.domain.model.Trip
import com.example.domain.repository.TripRepository
import com.example.domain.repository.WardrobeRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripsViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    val trips: StateFlow<List<Trip>> = tripRepository.observeAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTrip(id: Long) {
        viewModelScope.launch {
            tripRepository.deleteTrip(id)
        }
    }
}
