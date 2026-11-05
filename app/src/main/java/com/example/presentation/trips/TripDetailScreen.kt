package com.example.presentation.trips

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
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
import com.example.domain.model.TripPackingItem
import com.example.domain.model.TripDayOutfit
import com.example.presentation.common.WearWiseCard
import com.example.ui.theme.*

@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Packing List, 1: Daily Outfits
    var showAddItemDialog by remember { mutableStateOf(false) }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = state.trip?.destination?.uppercase() ?: "TRIP DETAILS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isCheckingWeather) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(2.dp),
                        color = CoutureAmber,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = viewModel::checkWeatherShiftAlert) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Check Live Weather Shift",
                            tint = CoutureAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = {
                    viewModel.deleteTrip {
                        Toast.makeText(context, "Trip deleted", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Trip",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CoutureAmber)
            }
        } else {
            val trip = state.trip
            if (trip == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Trip not found.", color = Color.White)
                }
            } else {
                val packedCount = trip.packingList.count { it.isPacked }
                val totalCount = trip.packingList.size
                val packingProgress = if (totalCount > 0) packedCount.toFloat() / totalCount else 0f

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header card
                    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlightTakeoff,
                                        contentDescription = null,
                                        tint = CoutureAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = trip.destination,
                                        color = CoutureAmber,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                }
                                Surface(
                                    color = RoyalElectricViolet.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = trip.occasion,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Dates: ${trip.departureDate} → ${trip.returnDate}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Weather Forecast: ${trip.weatherPreference}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )

                            if (trip.travelDistanceKm != null && trip.travelDurationHours != null) {
                                Spacer(Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val modeIcon = when (trip.transportMode) {
                                        "Flight" -> Icons.Default.Flight
                                        "Train" -> Icons.Default.Train
                                        "Bus" -> Icons.Default.DirectionsBus
                                        else -> Icons.Default.DirectionsCar
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = trip.transportMode,
                                            tint = CoutureAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${trip.transportMode} · ${trip.travelDistanceKm} km",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = "Est. Travel: ${trip.travelDurationHours} hrs",
                                        color = CoutureAmber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Packing Progress",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "$packedCount / $totalCount packed (${(packingProgress * 100).toInt()}%)",
                                    color = if (packedCount == totalCount && totalCount > 0) CoutureEmerald else CoutureAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { packingProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (packedCount == totalCount && totalCount > 0) CoutureEmerald else CoutureAmber,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }

                    // Re-Packing Alert Banner
                    if (trip.repackingAlert.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            color = WarningContainer,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoutureAmber)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = CoutureAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "RE-PACKING WEATHER ALERT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CoutureAmber
                                        )
                                    }
                                    IconButton(
                                        onClick = viewModel::dismissRepackingAlert,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = trip.repackingAlert,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    lineHeight = 16.sp
                                )

                                Button(
                                    onClick = viewModel::autoApplyRepackingRecommendations,
                                    colors = ButtonDefaults.buttonColors(containerColor = CoutureAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Auto-Add Missing Items to Checklist",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkNavyBg,
                        contentColor = CoutureAmber
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Packing List ($packedCount/$totalCount)") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Daily Outfits (${trip.itinerary.size})") }
                        )
                    }

                    if (selectedTab == 0) {
                        // Action row: Pack All, Unpack All, Add Item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { viewModel.setAllItemsPacked(true) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Pack All", fontSize = 11.sp, color = CoutureEmerald)
                                }
                                TextButton(
                                    onClick = { viewModel.setAllItemsPacked(false) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Unpack All", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                            Button(
                                onClick = { showAddItemDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Item", fontSize = 11.sp)
                            }
                        }

                        // Packing Checklist categorized
                        val standardCategories = listOf("Tops", "Bottoms", "Shoes", "Accessories", "Essentials")
                        val grouped = trip.packingList.groupBy { it.category }
                        val sortedCategories = standardCategories.filter { grouped.containsKey(it) } +
                                grouped.keys.filterNot { it in standardCategories }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            sortedCategories.forEach { category ->
                                val items = grouped[category].orEmpty()
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = category.uppercase(),
                                            color = CoutureAmber,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "${items.count { it.isPacked }}/${items.size}",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                items(items, key = { it.id }) { item ->
                                    PackingItemRow(
                                        item = item,
                                        onToggle = { viewModel.togglePackingItem(item.id) },
                                        onDelete = { viewModel.deletePackingItem(item.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Daily Itinerary
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(trip.itinerary, key = { it.dayNumber }) { dayOutfit ->
                                DayOutfitCard(dayOutfit = dayOutfit)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddCustomItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { category, name ->
                viewModel.addCustomPackingItem(category, name)
                showAddItemDialog = false
            }
        )
    }
}

@Composable
private fun AddCustomItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, name: String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Essentials") }
    val categories = listOf("Tops", "Bottoms", "Shoes", "Accessories", "Essentials")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Packing Item", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
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

                Text("Category:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = cat },
                            color = if (isSelected) CoutureAmber else CardElevated,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemName.isNotBlank()) {
                        onConfirm(selectedCategory, itemName)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoutureAmber)
            ) {
                Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = DarkNavyBg
    )
}

@Composable
private fun PackingItemRow(
    item: TripPackingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle),
        color = CardElevated,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (item.isPacked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (item.isPacked) CoutureEmerald else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = item.itemName,
                        color = if (item.isPacked) Color.White.copy(alpha = 0.45f) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.clothingItemId != null) {
                        Text(
                            text = "From your wardrobe",
                            color = CoutureAmber.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isPacked) {
                    Surface(
                        color = CoutureEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Packed",
                            color = CoutureEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove item",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayOutfitCard(dayOutfit: TripDayOutfit) {
    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CoutureAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Day ${dayOutfit.dayNumber} • ${dayOutfit.dateStr}",
                        color = CoutureAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = dayOutfit.outfitTitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = dayOutfit.rationale,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Outfit Pieces:",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            dayOutfit.clothingItemNames.forEach { itemName ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(RoyalElectricViolet)
                    )
                    Text(
                        text = itemName,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
