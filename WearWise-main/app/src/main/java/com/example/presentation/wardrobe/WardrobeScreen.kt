package com.example.presentation.wardrobe

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ClothingItem
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseClothingCard
import com.example.presentation.common.WearWiseDrawables
import com.example.presentation.common.WearWiseEmptyState
import com.example.presentation.common.WearWiseTopBar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CoutureAmber
import com.example.ui.theme.CoutureEmerald
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WardrobeViewModel(
    private val manageWardrobeUseCase: ManageWardrobeUseCase
) : ViewModel() {

    private val _items = MutableStateFlow<List<ClothingItem>>(emptyList())
    val items: StateFlow<List<ClothingItem>> = _items.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All Items")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadItems("All Items")
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        loadItems(category)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun loadItems(category: String) {
        viewModelScope.launch {
            manageWardrobeUseCase.getItemsByCategory(category).collect { list ->
                _items.value = list
            }
        }
    }

    fun addGarment(
        name: String,
        category: String,
        color: String,
        style: String,
        fabric: String,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val newItem = ClothingItem(
                name = name,
                category = category,
                color = color,
                style = style,
                season = "All-Season",
                formality = "Smart Casual",
                formalityScore = 8,
                fabric = fabric,
                imageRes = WearWiseDrawables.ItemOxford,
                imageUri = imageUri
            )
            manageWardrobeUseCase.addItem(newItem)
        }
    }

    fun deleteGarment(id: Long) {
        viewModelScope.launch {
            manageWardrobeUseCase.deleteItem(id)
        }
    }
}

@Composable
fun WardrobeScreen(
    viewModel: WardrobeViewModel,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "All Items",
        "Tops",
        "Bottoms",
        "Outerwear",
        "Shoes",
        "Knitwear",
        "Accessories"
    )

    val filteredItems = items.filter {
        searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.color.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .testTag("wardrobe_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WearWiseTopBar(
                title = "WEARWISE",
                subtitle = "AI COUTURE",
                userAvatarRes = WearWiseDrawables.UserAvatar,
                onAvatarClick = onNavigateToProfile
            )

            // Header (Image 3)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DIGITAL\nWARDROBE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B2032))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CoutureAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${items.size} DIGITIZED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoutureAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Neural telemetry of your archived fabrics, cuts, and silhouettes.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = {
                        Text(
                            text = "Search by color, season, fabric...",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Search",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner",
                                tint = RoyalElectricViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardElevated,
                        unfocusedContainerColor = CardElevated,
                        focusedBorderColor = RoyalElectricViolet,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("wardrobe_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) RoyalElectricViolet else CardElevated)
                                .border(1.dp, if (selected) RoyalElectricViolet else CardBorder, RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectCategory(cat) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("category_chip_$cat")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = cat.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 0.5.sp,
                                    color = if (selected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dominant Palette & Health Strip (Image 3)
                WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DOMINANT PALETTE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF1E293B)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4D6043)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF8FAFC)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Monochrome + Earth", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "CLOSET HEALTH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "94% Fit",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CoutureAmber
                            )
                        }
                    }
                }
            }

            // Grid of Garments
            if (filteredItems.isEmpty()) {
                WearWiseEmptyState(
                    title = "No garments in this category",
                    subtitle = "Tap '+ ADD GARMENT' below to add items to your closet using photo upload or camera."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        WearWiseClothingCard(
                            item = item,
                            onClick = {
                                Toast.makeText(context, "${item.name}: ${item.fabric}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Add Garment Floating Action Button (Image 3)
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = RoyalElectricViolet,
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .testTag("add_garment_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Garment",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ADD GARMENT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Dialog for adding garment
        if (showAddDialog) {
            AddGarmentDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, cat, col, style, fabric, uri ->
                    viewModel.addGarment(name, cat, col, style, fabric, uri)
                    showAddDialog = false
                    Toast.makeText(context, "Garment digitized successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun AddGarmentDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tops") }
    var color by remember { mutableStateOf("Deep Navy") }
    var style by remember { mutableStateOf("Smart Casual") }
    var fabric by remember { mutableStateOf("100% Breathable Cotton") }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }

    // Android Zero-permission photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Digitize New Garment",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Upload garment photo or choose from gallery for neural fabric analysis.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                        modifier = Modifier.weight(1f).border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = RoyalElectricViolet, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontSize = 12.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            // Camera / mock snapshot
                            selectedPhotoUri = "content://media/camera_capture"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                        modifier = Modifier.weight(1f).border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CoutureAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera", fontSize = 12.sp, color = TextPrimary)
                    }
                }

                if (selectedPhotoUri != null) {
                    Text(
                        text = "✓ Photo attached and analyzed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoutureEmerald
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Garment Name (e.g. Oxford Shirt)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Tops, Bottoms, Outerwear...)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color (e.g. Charcoal Grey, Deep Navy)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fabric,
                    onValueChange = { fabric = it },
                    label = { Text("Fabric & Composition") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        onAdd(name, category, color, style, fabric, selectedPhotoUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet)
            ) {
                Text("Digitize & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkNavyBg,
        tonalElevation = 6.dp
    )
}
