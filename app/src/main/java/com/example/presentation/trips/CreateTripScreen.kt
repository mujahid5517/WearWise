package com.example.presentation.trips

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.common.WearWiseCard
import com.example.ui.theme.*

@Composable
fun CreateTripScreen(
    viewModel: CreateTripViewModel,
    onNavigateBack: () -> Unit,
    onTripCreated: (Long) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.dismissError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
    ) {
        // App top navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkNavyBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "PLAN NEW TRIP",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = CoutureAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Trip Details & AI Styling",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Live destination weather & Gemini AI travel wardrobe",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Origin with Geocoding Auto-Suggestions
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = state.origin,
                            onValueChange = viewModel::updateOrigin,
                            label = { Text("Departure From (e.g. Dilla, London) [Optional]") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoutureAmber,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = CoutureAmber,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        if (state.originSuggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                color = CardElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoutureAmber.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    state.originSuggestions.forEach { loc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.selectOriginSuggestion(loc) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = CoutureAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = loc.displayName,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Destination with Geocoding Auto-Suggestions
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = state.destination,
                            onValueChange = viewModel::updateDestination,
                            label = { Text("Destination (e.g. Paris, Tokyo, Miami)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = viewModel::fetchLiveWeatherForCurrentDestination) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = "Fetch Live Weather",
                                        tint = CoutureAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoutureAmber,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = CoutureAmber,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Location suggestions dropdown
                        if (state.locationSuggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                color = CardElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoutureAmber.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    state.locationSuggestions.forEach { loc ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.selectLocationSuggestion(loc) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = CoutureAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = loc.displayName,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dates and Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.departureDate,
                            onValueChange = viewModel::updateDepartureDate,
                            label = { Text("Departure (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoutureAmber,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = CoutureAmber,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = state.returnDate,
                            onValueChange = viewModel::updateReturnDate,
                            label = { Text("Return (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoutureAmber,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = CoutureAmber,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    Surface(
                        color = RoyalElectricViolet.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = CoutureAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Estimated Trip Duration: ${state.durationDays} days (${state.durationDays} daily outfits)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Weather Preference & Live Forecast
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Weather / Climate Forecast",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            if (state.isFetchingWeather) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = CoutureAmber, strokeWidth = 2.dp)
                                    Text("Fetching Open-Meteo...", color = CoutureAmber, fontSize = 11.sp)
                                }
                            } else if (state.liveWeatherSummary != null) {
                                Surface(
                                    color = CoutureEmerald.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = CoutureEmerald, modifier = Modifier.size(12.dp))
                                        Text("Live Forecast Synced", color = CoutureEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.weatherPreference,
                            onValueChange = viewModel::updateWeatherPreference,
                            label = { Text("Weather / Climate Preference") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoutureAmber,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = CoutureAmber,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(CreateTripViewModel.WEATHER_OPTIONS) { opt ->
                                val selected = state.weatherPreference == opt
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.updateWeatherPreference(opt) },
                                    color = if (selected) CoutureAmber else CardElevated,
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) CoutureAmber else Color.White.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(
                                        text = opt,
                                        color = if (selected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Occasion
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.occasion,
                            onValueChange = viewModel::updateOccasion,
                            label = { Text("Travel Occasion") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoutureAmber,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = CoutureAmber,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(CreateTripViewModel.OCCASION_OPTIONS) { opt ->
                                val selected = state.occasion == opt
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { viewModel.updateOccasion(opt) },
                                    color = if (selected) RoyalElectricViolet else CardElevated,
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) RoyalElectricViolet else Color.White.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(
                                        text = opt,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Transport Mode Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Transport Mode",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CreateTripViewModel.TRANSPORT_MODE_OPTIONS.forEach { mode ->
                                val selected = state.transportMode == mode
                                val icon = when (mode) {
                                    "Flight" -> Icons.Default.Flight
                                    "Train" -> Icons.Default.Train
                                    "Bus" -> Icons.Default.DirectionsBus
                                    else -> Icons.Default.DirectionsCar
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.updateTransportMode(mode) },
                                    color = if (selected) CoutureAmber else CardElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) CoutureAmber else Color.White.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = mode,
                                            tint = if (selected) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = mode,
                                            color = if (selected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Availability Notice
                    Surface(
                        color = CardElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CoutureEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Items in laundry or marked unavailable will be safely excluded from recommendations.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.generateTrip { tripId ->
                                onTripCreated(tripId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CoutureAmber)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Gemini AI is styling your trip...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Text("✨ Generate AI Packing List & Outfits", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
