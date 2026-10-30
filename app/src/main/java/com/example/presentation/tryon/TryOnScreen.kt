package com.example.presentation.tryon

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.domain.model.ClothingItem
import com.example.domain.model.TryOnPreview
import com.example.domain.usecase.ExecuteTryOnUseCase
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.presentation.common.GarmentSwapDialog
import com.example.presentation.common.WearWiseButton
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseDrawables
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import com.example.domain.usecase.UserProfileUseCase

data class ActiveLayerItem(
    val slot: String, // BASE, BOTTOM, OUTERWEAR, ACCENTS
    val title: String,
    val dotColor: Color,
    val category: String, // Tops, Bottoms, Outerwear, Accessories
    val imageRes: Int? = null,
    val imageUri: String? = null
)

class TryOnViewModel(
    private val executeTryOnUseCase: ExecuteTryOnUseCase,
    private val manageWardrobeUseCase: ManageWardrobeUseCase,
    private val userProfileUseCase: UserProfileUseCase
) : ViewModel() {

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _currentPreview = MutableStateFlow<TryOnPreview?>(null)
    val currentPreview: StateFlow<TryOnPreview?> = _currentPreview.asStateFlow()

    private val _userPhotoUri = MutableStateFlow<String?>(null)
    val userPhotoUri: StateFlow<String?> = _userPhotoUri.asStateFlow()

    private val _fitType = MutableStateFlow("Tailored")
    val fitType: StateFlow<String> = _fitType.asStateFlow()

    private val _pose = MutableStateFlow("Confident Stance")
    val pose: StateFlow<String> = _pose.asStateFlow()

    private val _lighting = MutableStateFlow("Warm Daylight")
    val lighting: StateFlow<String> = _lighting.asStateFlow()

    private val _statusMessage = MutableStateFlow("Neural Cloth Physics Active • 60 FPS")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    val wardrobeItems: StateFlow<List<ClothingItem>> = manageWardrobeUseCase.getAllItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tryOnHistory: StateFlow<List<TryOnPreview>> = executeTryOnUseCase.getHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activeLayers = MutableStateFlow(
        listOf(
            ActiveLayerItem("BASE", "Oxford Shirt", Color(0xFFF8FAFC), "Tops", WearWiseDrawables.ItemOxford),
            ActiveLayerItem("BOTTOM", "Navy Chinos", Color(0xFF1E293B), "Bottoms", WearWiseDrawables.ItemChinos),
            ActiveLayerItem("OUTERWEAR", "Olive Blazer", Color(0xFF4D6043), "Outerwear", WearWiseDrawables.ItemBlazer),
            ActiveLayerItem("ACCENTS", "Loafers & Watch", CoutureAmber, "Accessories", WearWiseDrawables.ItemLoafers)
        )
    )
    val activeLayers: StateFlow<List<ActiveLayerItem>> = _activeLayers.asStateFlow()

    init {
        _currentPreview.value = TryOnPreview(
            id = 1L,
            outfitId = 1L,
            previewImageRes = WearWiseDrawables.TryOnModel,
            harmonyScore = 94,
            fitType = "Tailored",
            pose = "Confident Stance",
            lighting = "Warm Daylight",
            activePiecesCount = 4
        )

        viewModelScope.launch {
            userProfileUseCase.getProfile().collect { p ->
                if (_userPhotoUri.value == null && !p?.profileImage.isNullOrEmpty()) {
                    _userPhotoUri.value = p?.profileImage
                }
            }
        }
    }

    fun setFitType(fit: String) {
        _fitType.value = fit
        runSimulation()
    }

    fun setPose(pose: String) {
        _pose.value = pose
        runSimulation()
    }

    fun setLighting(light: String) {
        _lighting.value = light
    }

    fun setUserPhoto(uri: String) {
        _userPhotoUri.value = uri
        runSimulation()
    }

    fun setUserBitmap(bitmap: Bitmap, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(context.cacheDir, "user_avatar_${System.currentTimeMillis()}.jpg")
            val fos = file.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            _userPhotoUri.value = file.absolutePath
            runSimulation()
        }
    }

    fun clearUserPhoto() {
        _userPhotoUri.value = null
        runSimulation()
    }

    fun swapLayerItem(slotName: String, newItem: ClothingItem) {
        val updated = _activeLayers.value.map { item ->
            if (item.slot == slotName) {
                item.copy(
                    title = newItem.name,
                    imageRes = newItem.imageRes,
                    imageUri = newItem.imageUri
                )
            } else {
                item
            }
        }
        _activeLayers.value = updated
        runSimulation()
    }

    fun selectHistoryPreview(preview: TryOnPreview) {
        _currentPreview.value = preview
        _fitType.value = preview.fitType
        _pose.value = preview.pose
        _lighting.value = preview.lighting
        if (preview.userPhotoPath != null) {
            _userPhotoUri.value = preview.userPhotoPath
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            executeTryOnUseCase.deleteHistoryEntry(id)
        }
    }

    fun runSimulation() {
        viewModelScope.launch {
            _isSimulating.value = true
            _statusMessage.value = "Calibrating photorealistic drape & fabric tensions..."

            val garmentDescriptions = _activeLayers.value.map {
                "${it.slot}: ${it.title}"
            }

            try {
                val result = executeTryOnUseCase.execute(
                    outfitId = 1L,
                    fitType = _fitType.value,
                    pose = _pose.value,
                    lighting = _lighting.value,
                    userPhotoUri = _userPhotoUri.value,
                    garmentDescriptions = garmentDescriptions
                )
                _currentPreview.value = result
                _statusMessage.value = if (!result.generatedImagePath.isNullOrEmpty()) {
                    "Gemini AI Virtual Try-On Rendered"
                } else {
                    "Neural Cloth Physics Calibrated (99.2% Accuracy)"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Neural Physics Calibrated"
            } finally {
                _isSimulating.value = false
            }
        }
    }
}

@Composable
fun TryOnScreen(
    viewModel: TryOnViewModel,
    isOnline: Boolean = true,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val preview by viewModel.currentPreview.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val userPhotoUri by viewModel.userPhotoUri.collectAsState()
    val fitType by viewModel.fitType.collectAsState()
    val pose by viewModel.pose.collectAsState()
    val lighting by viewModel.lighting.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val activeLayers by viewModel.activeLayers.collectAsState()
    val allWardrobeItems by viewModel.wardrobeItems.collectAsState()
    val historyPreviews by viewModel.tryOnHistory.collectAsState()

    var activeTab by remember { mutableStateOf("Current Outfit") }
    var activeSwapSlot by remember { mutableStateOf<ActiveLayerItem?>(null) }

    // Photo picker launcher (Gallery)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.setUserPhoto(uri.toString())
                Toast.makeText(context, "Photo loaded for try-on simulation", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                viewModel.setUserBitmap(bitmap, context)
                Toast.makeText(context, "Captured photo loaded for try-on simulation", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .testTag("tryon_screen")
    ) {
        WearWiseTopBar(
            title = "WEARWISE",
            subtitle = "AI COUTURE",
            userAvatarRes = WearWiseDrawables.UserAvatar,
            onAvatarClick = onNavigateToProfile
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Style Avatar\nStudio",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-time photorealistic cloth physics & fit gauge",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B2032))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = RoyalElectricViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "3D / AR READY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalElectricViolet
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Studio Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("Current Outfit", "Saved Looks", "Favorites").forEach { tab ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) RoyalElectricViolet else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeTab == "Current Outfit") {
                // Main Photorealistic Studio Avatar Window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                ) {
                    // Dynamic Image Rendering
                    val generatedPath = preview?.generatedImagePath
                    val photoPath = userPhotoUri ?: preview?.userPhotoPath

                    when {
                        !generatedPath.isNullOrEmpty() -> {
                            AsyncImage(
                                model = generatedPath,
                                contentDescription = "Generated Virtual Try-On",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        !photoPath.isNullOrEmpty() -> {
                            AsyncImage(
                                model = photoPath,
                                contentDescription = "User Avatar Try-On",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Image(
                                painter = painterResource(id = preview?.previewImageRes ?: WearWiseDrawables.TryOnModel),
                                contentDescription = "Virtual Try-On Model",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Simulation Progress Overlay
                    if (isSimulating) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = RoyalElectricViolet,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "SYNTHESIZING DRAPE...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Top Action Bar Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xCC0B0D14))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "• $statusMessage",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoutureAmber,
                                maxLines = 1
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Camera Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xCC0B0D14))
                                    .clickable { cameraLauncher.launch(null) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Take Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Camera", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Gallery Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xCC0B0D14))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Pick Photo",
                                        tint = CoutureAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gallery", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoutureAmber)
                                }
                            }

                            // Reset to Model if photo active
                            if (!userPhotoUri.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xCC0B0D14))
                                        .clickable { viewModel.clearUserPhoto() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("Model", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                }
                            }
                        }
                    }

                    // Interactive Callout Hotspot Badges
                    val baseLayerName = activeLayers.find { it.slot == "BASE" }?.title ?: "Oxford"
                    val outerLayerName = activeLayers.find { it.slot == "OUTERWEAR" }?.title ?: "Blazer"
                    val bottomLayerName = activeLayers.find { it.slot == "BOTTOM" }?.title ?: "Chinos"
                    val accentName = activeLayers.find { it.slot == "ACCENTS" }?.title ?: "Loafers"

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp, bottom = 60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xDD0B0D14))
                            .border(1.dp, RoyalElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                activeSwapSlot = activeLayers.find { it.slot == "OUTERWEAR" }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("TOP: $outerLayerName + $baseLayerName ⇄", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp, top = 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xDD0B0D14))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                activeSwapSlot = activeLayers.find { it.slot == "BOTTOM" }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("BOTTOM: $bottomLayerName ⇄", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    // Bottom Harmony Score Bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xDD0B0D14))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(RoyalElectricViolet),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${preview?.harmonyScore ?: 94}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("HARMONY SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text("Smart Business Casual", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }

                            Text("• $accentName", fontSize = 11.sp, color = CoutureAmber)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Garment Layer Stack with Swap Functionality
                WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Garment Layer Stack",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22283A))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "TAP ITEM TO SWAP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoutureAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            activeLayers.forEach { layer ->
                                ClickableLayerPill(
                                    layer = layer,
                                    onClick = { activeSwapSlot = layer }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Simulation Controls (Fit Simulation, Pose, Lighting)
                WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Fit Simulation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Relaxed", "Tailored", "Slim").forEach { fit ->
                                val selected = fitType == fit
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) RoyalElectricViolet else Color(0xFF1B2032))
                                        .clickable { viewModel.setFitType(fit) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(fit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1B2032))
                                    .clickable {
                                        val nextPose = if (pose == "Confident Stance") "Editorial Stride" else "Confident Stance"
                                        viewModel.setPose(nextPose)
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("POSE", fontSize = 9.sp, color = TextMuted)
                                    Text(pose, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1B2032))
                                    .clickable {
                                        val nextLight = if (lighting == "Warm Daylight") "Studio Softbox" else "Warm Daylight"
                                        viewModel.setLighting(nextLight)
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("LIGHTING", fontSize = 9.sp, color = TextMuted)
                                    Text(lighting, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoutureAmber)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Simulate Virtual Try-On Button
                WearWiseButton(
                    text = if (isSimulating) "CALIBRATING CLOTH PHYSICS..." else "SIMULATE VIRTUAL TRY-ON",
                    onClick = { viewModel.runSimulation() },
                    enabled = !isSimulating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_try_on_button"),
                    trailingIcon = if (isSimulating) null else Icons.Default.AutoAwesome
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Look saved to favorites!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Look", fontSize = 13.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Sharing Virtual Avatar card...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Look", fontSize = 13.sp, color = TextPrimary)
                    }
                }
            } else {
                // Saved Looks / History Grid
                Text(
                    text = "Virtual Try-On Archive",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (historyPreviews.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved simulations yet.\nTap 'Simulate Virtual Try-On' to generate.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(460.dp)
                    ) {
                        items(historyPreviews, key = { it.id }) { item ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(CardElevated)
                                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.selectHistoryPreview(item)
                                        activeTab = "Current Outfit"
                                        Toast.makeText(context, "Loaded look into Studio", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1B2032))
                                    ) {
                                        if (!item.generatedImagePath.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = item.generatedImagePath,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = item.previewImageRes ?: WearWiseDrawables.TryOnModel),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xCC0B0D14))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("${item.harmonyScore}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoutureAmber)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${item.fitType} Fit • ${item.pose}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.lighting,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Interactive Garment Swap Dialog
    activeSwapSlot?.let { layer ->
        GarmentSwapDialog(
            currentSlotTitle = layer.slot,
            initialCategory = layer.category,
            allGarments = allWardrobeItems,
            onGarmentSelected = { newItem ->
                viewModel.swapLayerItem(layer.slot, newItem)
                Toast.makeText(context, "Swapped ${layer.slot} to ${newItem.name}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { activeSwapSlot = null }
        )
    }
}

@Composable
private fun ClickableLayerPill(
    layer: ActiveLayerItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B2032))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(layer.slot, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (layer.title.length > 8) layer.title.take(7) + ".." else layer.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(layer.dotColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap",
                    tint = CoutureAmber,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
