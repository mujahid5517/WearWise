package com.example.presentation.tryon

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.media.TryOnFileStore
import com.example.domain.model.ClothingItem
import com.example.domain.model.SelectedTryOnGarment
import com.example.domain.model.TryOnPreview
import com.example.presentation.common.WearWiseButton
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseDrawables
import com.example.presentation.common.WearWiseTopBar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CoutureAmber
import com.example.ui.theme.CoutureEmerald
import com.example.ui.theme.CoutureRose
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TryOnScreen(
    viewModel: TryOnViewModel,
    isOnline: Boolean = true,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preview by viewModel.currentPreview.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val userPhotoUri by viewModel.userPhotoUri.collectAsState()
    val fitType by viewModel.fitType.collectAsState()
    val pose by viewModel.pose.collectAsState()
    val lighting by viewModel.lighting.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val garments by viewModel.garments.collectAsState()
    val wardrobeItems by viewModel.wardrobeItems.collectAsState()
    val outfits by viewModel.outfits.collectAsState()
    val historyPreviews by viewModel.tryOnHistory.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val photoError by viewModel.photoError.collectAsState()
    val privacyAccepted by viewModel.privacyAccepted.collectAsState()

    var activeTab by remember { mutableStateOf("Studio") }
    var clothingCategory by remember { mutableStateOf("All") }
    var comparisonMode by remember { mutableStateOf("Slider") }
    var fullscreenModel by remember { mutableStateOf<Pair<Any, String>?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var garmentUploadCategory by remember { mutableStateOf("Tops") }
    val colorScheme = MaterialTheme.colorScheme
    val fileStore = remember { TryOnFileStore(context) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.importPersonPhoto(uri.toString())
    }

    val garmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.importGarmentPhoto(uri.toString(), garmentUploadCategory)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.importPersonPhoto(uri.toString())
        }
    }

    fun launchCameraCapture() {
        val (uri, _) = fileStore.createCameraOutputUri()
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            viewModel.setPhotoError("Camera permission is required to take a photo. You can still choose an image from the gallery.")
        }
    }

    fun launchCamera() {
        if (cameraPermission.status.isGranted) {
            launchCameraCapture()
        } else {
            cameraPermission.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
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
            Text(
                text = "AI Virtual Try-On",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1. Your photo  2. Clothing  3. Generate  4. Compare",
                fontSize = 12.sp,
                color = TextSecondary
            )

            if (!isOnline) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33F43F5E))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = CoutureRose, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Internet is required to create a new virtual try-on.",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            StudioTabs(activeTab) { activeTab = it }
            Spacer(modifier = Modifier.height(16.dp))

            if (activeTab == "Studio") {
                PhotoStepCard(
                    userPhotoUri = userPhotoUri,
                    photoError = photoError,
                    onCamera = { launchCamera() },
                    onGallery = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRotate = { viewModel.rotatePersonPhoto(90) },
                    onCrop = { showCropDialog = true },
                    onRemove = { viewModel.clearUserPhoto() },
                    onPreview = { model -> fullscreenModel = model to "BEFORE — Original Photo" }
                )

                Spacer(modifier = Modifier.height(14.dp))
                ClothingStepCard(
                    garments = garments,
                    wardrobeItems = wardrobeItems,
                    outfits = outfits,
                    category = clothingCategory,
                    onCategory = { clothingCategory = it },
                    onGarment = { viewModel.addGarment(it) },
                    onOutfit = { viewModel.applyOutfit(it) },
                    onRemove = { viewModel.removeGarment(it) },
                    onUpload = { category ->
                        garmentUploadCategory = category
                        garmentPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
                StudioControlsCard(
                    fitType = fitType,
                    pose = pose,
                    lighting = lighting,
                    onFit = viewModel::setFitType,
                    onPose = viewModel::setPose,
                    onLighting = viewModel::setLighting
                )

                Spacer(modifier = Modifier.height(16.dp))
                WearWiseButton(
                    text = if (isGenerating) "AI IS CREATING YOUR OUTFIT..." else "GENERATE VIRTUAL TRY-ON",
                    onClick = {
                        if (!privacyAccepted) {
                            showPrivacyDialog = true
                        } else {
                            viewModel.generateTryOn()
                        }
                    },
                    enabled = !isGenerating && isOnline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_try_on_button"),
                    trailingIcon = if (isGenerating) null else Icons.Default.AutoAwesome
                )

                AnimatedVisibility(visible = isGenerating) {
                    GenerationProgressCard(
                        statusMessage = statusMessage,
                        onCancel = { viewModel.cancelGeneration() }
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ErrorBanner(errorMessage!!) {
                        viewModel.clearError()
                        viewModel.generateTryOn()
                    }
                }

                val generatedPath = preview?.generatedImagePath
                val beforePath = userPhotoUri ?: preview?.userPhotoPath
                if (!generatedPath.isNullOrBlank() && !beforePath.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    if (statusMessage == "Your virtual outfit has been generated successfully!") {
                        Text(
                            text = statusMessage,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CoutureEmerald
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ResultCard(
                        beforePath = beforePath,
                        afterPath = generatedPath,
                        preview = preview!!,
                        comparisonMode = comparisonMode,
                        onMode = { comparisonMode = it },
                        onFullscreen = { model, title -> fullscreenModel = model to title },
                        onSaveLook = { viewModel.toggleSavedLook() },
                        onDownload = {
                            scope.launch {
                                val result = viewModel.saveToGallery()
                                Toast.makeText(
                                    context,
                                    if (result.isSuccess) "Saved to Pictures/WearWise" else (result.exceptionOrNull()?.message ?: "Download failed"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onShare = {
                            val uri = viewModel.shareUriOrNull()
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share virtual try-on"))
                            }
                        }
                    )
                }
            } else {
                HistoryGrid(
                    items = if (activeTab == "Saved Looks") {
                        historyPreviews.filter { it.isSavedLook && !it.generatedImagePath.isNullOrBlank() }
                    } else {
                        historyPreviews.filter { !it.generatedImagePath.isNullOrBlank() }
                    },
                    emptyText = if (activeTab == "Saved Looks") {
                        "No saved looks yet. Generate a try-on, then tap Save Look."
                    } else {
                        "No previous results yet. Generate a virtual try-on to build history."
                    },
                    onSelect = {
                        viewModel.selectHistoryPreview(it)
                        activeTab = "Studio"
                    },
                    onDelete = { viewModel.deleteHistoryEntry(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    fullscreenModel?.let { (model, title) ->
        FullscreenImagePreview(model = model, title = title) { fullscreenModel = null }
    }

    if (showCropDialog && !userPhotoUri.isNullOrBlank()) {
        PhotoCropDialog(
            photoPath = userPhotoUri!!,
            onDismiss = { showCropDialog = false },
            onConfirm = { left, top, right, bottom ->
                showCropDialog = false
                viewModel.cropPersonPhoto(left, top, right, bottom)
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Photos are sent to Gemini", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your personal photo and selected clothing images will be sent to Google Gemini to generate a virtual try-on. They are not uploaded until you tap Generate. You can delete results from history at any time."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.acceptPrivacy()
                    showPrivacyDialog = false
                    viewModel.generateTryOn()
                }) { Text("Generate") }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StudioTabs(activeTab: String, onTab: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardElevated)
            .padding(4.dp)
    ) {
        listOf("Studio", "History", "Saved Looks").forEach { tab ->
            val selected = activeTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) RoyalElectricViolet else Color.Transparent)
                    .clickable { onTab(tab) }
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
}

@Composable
private fun PhotoStepCard(
    userPhotoUri: String?,
    photoError: String?,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRotate: () -> Unit,
    onCrop: () -> Unit,
    onRemove: () -> Unit,
    onPreview: (Any) -> Unit
) {
    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("1. Your photo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardElevated)
                    .clickable(enabled = userPhotoUri != null) {
                        userPhotoUri?.let(onPreview)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!userPhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = userPhotoUri,
                        contentDescription = "Personal photo preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        "Take or choose a well-lit full-body or upper-body photo.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            if (!photoError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(photoError, color = CoutureRose, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionChip("Camera", Icons.Default.CameraAlt, Modifier.weight(1f), onCamera)
                ActionChip("Gallery", Icons.Default.PhotoLibrary, Modifier.weight(1f), onGallery)
            }
            if (!userPhotoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionChip("Rotate", Icons.Default.Rotate90DegreesCcw, Modifier.weight(1f), onRotate)
                    ActionChip("Crop 3:4", Icons.Default.Crop, Modifier.weight(1f), onCrop)
                    ActionChip("Remove", Icons.Default.Close, Modifier.weight(1f), onRemove)
                }
            }
        }
    }
}

@Composable
private fun ClothingStepCard(
    garments: List<SelectedTryOnGarment>,
    wardrobeItems: List<ClothingItem>,
    outfits: List<com.example.domain.model.Outfit>,
    category: String,
    onCategory: (String) -> Unit,
    onGarment: (ClothingItem) -> Unit,
    onOutfit: (com.example.domain.model.Outfit) -> Unit,
    onRemove: (SelectedTryOnGarment) -> Unit,
    onUpload: (String) -> Unit
) {
    val context = LocalContext.current
    val categories = listOf("All", "Tops", "Bottoms", "Dresses", "Skirts", "Outerwear", "Shoes", "Outfits")
    val filtered = wardrobeItems.filter {
        category == "All" || category == "Outfits" || it.category.equals(category, true) ||
            (category == "Tops" && it.category.contains("top", true)) ||
            (category == "Bottoms" && it.category.contains("bottom", true))
    }

    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("2. Select clothing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val selected = category == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) RoyalElectricViolet else CardElevated)
                            .clickable { onCategory(cat) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { onUpload(if (category == "All" || category == "Outfits") "Tops" else category) }) {
                Text("Upload clothing photo", color = CoutureAmber, fontSize = 12.sp)
            }
            if (garments.isNotEmpty()) {
                Text("Selected", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    garments.forEach { garment ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardElevated)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(
                                    model = garment.imageUri ?: garment.imageRes ?: garment.catalogAssetName?.let {
                                        context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 }
                                    },
                                    contentDescription = garment.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Text(
                                    garment.name,
                                    fontSize = 10.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(64.dp)
                                )
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = CoutureRose,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemove(garment) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (category == "Outfits") {
                if (outfits.isEmpty()) {
                    Text("No saved outfits yet.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    outfits.take(8).forEach { outfit ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardElevated)
                                .clickable { onOutfit(outfit) }
                                .padding(10.dp)
                        ) {
                            Text(outfit.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                val display = if (category == "All") wardrobeItems.take(12) else filtered.take(12)
                if (display.isEmpty()) {
                    Text("No garments in this category. Upload a clothing photo instead.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        display.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                row.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CardElevated)
                                            .clickable { onGarment(item) }
                                            .padding(6.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            AsyncImage(
                                                model = item.imageUri ?: item.imageUrl ?: item.imageRes ?: item.catalogAssetName?.let {
                                                    context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 }
                                                },
                                                contentDescription = item.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .height(64.dp)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Text(item.name, fontSize = 10.sp, color = TextPrimary, maxLines = 1)
                                        }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioControlsCard(
    fitType: String,
    pose: String,
    lighting: String,
    onFit: (String) -> Unit,
    onPose: (String) -> Unit,
    onLighting: (String) -> Unit
) {
    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("3. Fit and lighting", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Relaxed", "Tailored", "Slim").forEach { fit ->
                    val selected = fitType == fit
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) RoyalElectricViolet else CardElevated)
                            .clickable { onFit(fit) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(fit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ControlTile(
                    label = "POSE",
                    value = pose,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onPose(if (pose == "Natural Stance") "Confident Stance" else "Natural Stance")
                    }
                )
                ControlTile(
                    label = "LIGHTING",
                    value = lighting,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onLighting(
                            if (lighting == "Keep original lighting") "Studio Softbox" else "Keep original lighting"
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlTile(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardElevated)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Text(label, fontSize = 9.sp, color = TextMuted)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun ResultCard(
    beforePath: String,
    afterPath: String,
    preview: TryOnPreview,
    comparisonMode: String,
    onMode: (String) -> Unit,
    onFullscreen: (Any, String) -> Unit,
    onSaveLook: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("4. Before & after", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Slider", "Side by side", "After").forEach { mode ->
                    val selected = comparisonMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) RoyalElectricViolet else CardElevated)
                            .clickable { onMode(mode) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(mode, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (comparisonMode) {
                "Slider" -> BeforeAfterSlider(
                    beforeModel = beforePath,
                    afterModel = afterPath,
                    modifier = Modifier.fillMaxWidth().height(420.dp)
                )
                "Side by side" -> SideBySideComparison(
                    beforeModel = beforePath,
                    afterModel = afterPath,
                    modifier = Modifier.fillMaxWidth()
                )
                else -> Box {
                    AsyncImage(
                        model = afterPath,
                        contentDescription = "AFTER — AI Virtual Try-On",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onFullscreen(afterPath, "AFTER — AI Virtual Try-On") }
                    )
                    ComparisonBadge(
                        text = "AFTER — AI Virtual Try-On",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionChip("Zoom", Icons.Default.ZoomIn, Modifier.weight(1f)) {
                    onFullscreen(afterPath, "AFTER — AI Virtual Try-On")
                }
                ActionChip(
                    if (preview.isSavedLook) "Saved" else "Save Look",
                    if (preview.isSavedLook) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    Modifier.weight(1f),
                    onSaveLook
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionChip("Download", Icons.Default.Download, Modifier.weight(1f), onDownload)
                ActionChip("Share", Icons.Default.Share, Modifier.weight(1f), onShare)
            }
        }
    }
}

@Composable
private fun HistoryGrid(
    items: List<TryOnPreview>,
    emptyText: String,
    onSelect: (TryOnPreview) -> Unit,
    onDelete: (Long) -> Unit
) {
    Text("Virtual Try-On Archive", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(12.dp))
    if (items.isEmpty()) {
        Text(emptyText, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(460.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .clickable { onSelect(item) }
                        .padding(8.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardElevated)
                        ) {
                            if (!item.generatedImagePath.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.generatedImagePath,
                                    contentDescription = "Saved try-on",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xCC0B0D14))
                                    .clickable { onDelete(item.id) }
                                    .padding(6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            item.garmentSummary ?: "${item.fitType} • ${item.pose}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardElevated)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = CoutureAmber, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33F43F5E))
            .padding(12.dp)
    ) {
        Text(message, color = TextPrimary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        TextButton(onClick = onRetry) {
            Text("Retry", color = CoutureEmerald, fontWeight = FontWeight.Bold)
        }
    }
}
