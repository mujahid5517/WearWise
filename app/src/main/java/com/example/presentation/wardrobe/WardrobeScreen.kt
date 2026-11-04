package com.example.presentation.wardrobe

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.data.local.ApiKeyManager
import com.example.data.media.TryOnFileStore
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateContentRequest
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import com.example.domain.model.ClothingItem
import com.example.domain.model.WardrobeTaxonomy
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.presentation.common.WearWiseDrawables
import com.example.presentation.common.WearWiseTopBar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CoutureAmber
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WardrobeSuggestion(val category: String, val subcategory: String, val color: String, val pattern: String, val description: String)

class WardrobeViewModel(
    private val manageWardrobeUseCase: ManageWardrobeUseCase,
    private val fileStore: TryOnFileStore? = null,
    private val apiKeyManager: ApiKeyManager? = null
) : ViewModel() {
    val items: StateFlow<List<ClothingItem>> = manageWardrobeUseCase.getAllItems()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())
    val syncMessage: StateFlow<String?> = manageWardrobeUseCase.syncMessage
    val uploadProgress: StateFlow<Float?> = manageWardrobeUseCase.uploadProgress
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _suggesting = MutableStateFlow(false)
    val suggesting = _suggesting.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun clearError() { _error.value = null }
    fun setError(message: String) { _error.value = message }
    suspend fun persistPhoto(uri: String): String = manageWardrobeUseCase.persistClothingImage(uri)

    fun save(item: ClothingItem, existingId: Long? = null, onDone: (Boolean) -> Unit) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            try {
                if (existingId == null) manageWardrobeUseCase.addItem(item)
                else manageWardrobeUseCase.updateItem(item.copy(id = existingId))
                onDone(true)
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not save the clothing item."
                onDone(false)
            } finally { _saving.value = false }
        }
    }

    fun toggleFavorite(item: ClothingItem) {
        viewModelScope.launch { manageWardrobeUseCase.toggleFavorite(item.id, !item.isFavorite) }
    }
    fun delete(item: ClothingItem) { viewModelScope.launch { manageWardrobeUseCase.deleteItem(item.id) } }

    fun suggestFromPhoto(path: String, onSuggestion: (WardrobeSuggestion) -> Unit) {
        viewModelScope.launch {
            _suggesting.value = true
            _error.value = null
            try {
                val key = apiKeyManager?.getEffectiveApiKey().orEmpty()
                require(key.isNotBlank()) { "Add a Gemini API key in Profile to request clothing suggestions. You can still enter details manually." }
                val image = withContext(Dispatchers.IO) { fileStore?.encodeForApi(path) }
                    ?: error("Image analysis is unavailable.")
                val prompt = "Analyze this clothing photo. Reply with exactly five lines, no markdown: category (one of Tops, Bottoms, Outerwear, Dresses and One-Piece, Shoes, Accessories); subcategory; dominant color; visible pattern (or Solid); one concise visual description. Do not infer brand or size."
                val result = GeminiClient.apiService.generateTextContent(
                    key,
                    GeminiGenerateContentRequest(
                        contents = listOf(GeminiContent(listOf(
                            GeminiPart(text = prompt),
                            GeminiPart(inlineData = GeminiInlineData(image.mimeType, image.base64Data))
                        )))
                ))
                val lines = result.candidates?.firstOrNull()?.content?.parts.orEmpty()
                    .mapNotNull { it.text?.trim() }.firstOrNull().orEmpty().lines().map { it.trim() }
                require(lines.size >= 5) { "AI did not return usable suggestions. Enter the details manually." }
                val categoryLine = lines[0].substringAfter(":", lines[0]).trim()
                val category = WardrobeTaxonomy.categories.keys.firstOrNull { categoryLine.equals(it, true) }
                    ?: WardrobeTaxonomy.categories.keys.firstOrNull { categoryLine.contains(it, true) }
                    ?: when {
                        categoryLine.contains("shirt", true) || categoryLine.contains("blouse", true) -> "Tops"
                        categoryLine.contains("jean", true) || categoryLine.contains("trouser", true) || categoryLine.contains("short", true) -> "Bottoms"
                        categoryLine.contains("shoe", true) || categoryLine.contains("sneaker", true) || categoryLine.contains("boot", true) -> "Shoes"
                        categoryLine.contains("dress", true) || categoryLine.contains("jumpsuit", true) -> "Dresses and One-Piece"
                        categoryLine.contains("coat", true) || categoryLine.contains("jacket", true) || categoryLine.contains("blazer", true) -> "Outerwear"
                        categoryLine.contains("accessor", true) || categoryLine.contains("bag", true) -> "Accessories"
                        else -> error("AI category was unclear. Enter the category manually.")
                    }
                onSuggestion(WardrobeSuggestion(category, lines[1], lines[2], lines[3], lines.drop(4).joinToString(" ")))
            } catch (e: Exception) {
                _error.value = e.message ?: "Image analysis failed. Enter the details manually."
            } finally { _suggesting.value = false }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WardrobeScreen(
    viewModel: WardrobeViewModel,
    onNavigateToProfile: () -> Unit,
    onUseInTryOn: (ClothingItem) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items by viewModel.items.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val suggesting by viewModel.suggesting.collectAsState()
    val error by viewModel.error.collectAsState()
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var subcategory by remember { mutableStateOf("All types") }
    var color by remember { mutableStateOf("All colors") }
    var season by remember { mutableStateOf("All seasons") }
    var occasion by remember { mutableStateOf("All occasions") }
    var brand by remember { mutableStateOf("All brands") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf("Recently added") }
    var selectedItem by remember { mutableStateOf<ClothingItem?>(null) }
    var editingItem by remember { mutableStateOf<ClothingItem?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
    var pendingCamera by remember { mutableStateOf<Uri?>(null) }
    val fileStore = remember { TryOnFileStore(context) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { runCatching { viewModel.persistPhoto(uri.toString()) }
            .onSuccess { selectedPhotoPath = it }
            .onFailure { viewModel.setError(it.message ?: "Could not load that photo.") } }
    }
    val cameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCamera
        if (success && uri != null) scope.launch { runCatching { viewModel.persistPhoto(uri.toString()) }
            .onSuccess { selectedPhotoPath = it }
            .onFailure { viewModel.setError(it.message ?: "Could not save the camera photo.") } }
    }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) {
            val (uri, _) = fileStore.createCameraOutputUri()
            pendingCamera = uri
            cameraPicker.launch(uri)
        } else viewModel.setError("Camera permission was denied. You can still choose a clothing photo from the gallery.")
    }
    val categories = listOf("All") + (WardrobeTaxonomy.categories.keys + items.map { it.category }).distinct()
    val filtered = remember(items, search, category, subcategory, color, season, occasion, brand, favoritesOnly, sort) {
        val matches = items.filter { item ->
            val queryMatch = search.isBlank() || listOf(item.name, item.brand, item.category, item.subcategory, item.color)
                .any { it.contains(search, true) }
            queryMatch && (category == "All" || item.category.equals(category, true)) &&
                (subcategory == "All types" || item.subcategory.equals(subcategory, true)) &&
                (color == "All colors" || item.color.equals(color, true)) &&
                (season == "All seasons" || item.season.equals(season, true)) &&
                (occasion == "All occasions" || item.occasion.equals(occasion, true)) &&
                (brand == "All brands" || item.brand.equals(brand, true)) && (!favoritesOnly || item.isFavorite)
        }
        when (sort) {
            "Alphabetical" -> matches.sortedBy { it.name.lowercase() }
            "Oldest first" -> matches.sortedBy { it.createdAt }
            "Most worn" -> matches.sortedByDescending { it.wearCount }
            else -> matches.sortedByDescending { it.createdAt }
        }
    }

    Column(Modifier.fillMaxSize().background(DarkNavyBg).testTag("wardrobe_screen")) {
        WearWiseTopBar(title = "WEARWISE", subtitle = "MY WARDROBE", userAvatarRes = WearWiseDrawables.UserAvatar, onAvatarClick = onNavigateToProfile)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("My Wardrobe", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("${items.size} pieces · ${items.count { it.isFavorite }} favorites", color = TextSecondary, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { showCatalog = true }, colors = ButtonDefaults.textButtonColors(contentColor = CoutureAmber)) { Text("Catalog") }
                    Button(onClick = { editingItem = null; selectedPhotoPath = null; viewModel.clearError(); showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add clothing")
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Tops", items.count { it.category.equals("Tops", true) }, Modifier.weight(1f))
                StatPill("Bottoms", items.count { it.category.equals("Bottoms", true) }, Modifier.weight(1f))
                StatPill("Shoes", items.count { it.category.equals("Shoes", true) }, Modifier.weight(1f))
                StatPill("Extras", items.count { it.category.equals("Accessories", true) }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().height(54.dp).testTag("wardrobe_search_input"),
                placeholder = { Text("Search name, brand, category, color", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardElevated, unfocusedContainerColor = CardElevated,
                    focusedBorderColor = RoyalElectricViolet, unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                ), singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { value -> FilterChip(value, category == value) { category = value } }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChoiceMenu("Type", listOf("All types") + items.filter { category == "All" || it.category.equals(category, true) }.map { it.subcategory }.filter { it.isNotBlank() }.distinct(), subcategory) { subcategory = it }
                ChoiceMenu("Color", listOf("All colors") + items.map { it.color }.filter { it.isNotBlank() }.distinct(), color) { color = it }
                ChoiceMenu("Season", listOf("All seasons") + WardrobeTaxonomy.seasons, season) { season = it }
                ChoiceMenu("Occasion", listOf("All occasions") + WardrobeTaxonomy.occasions, occasion) { occasion = it }
                ChoiceMenu("Brand", listOf("All brands") + items.map { it.brand }.filter { it.isNotBlank() }.distinct(), brand) { brand = it }
                ChoiceMenu("Sort", listOf("Recently added", "Alphabetical", "Oldest first", "Most worn"), sort) { sort = it }
                FilterChip(if (favoritesOnly) "♥ Favorites" else "♡ Favorites", favoritesOnly) { favoritesOnly = !favoritesOnly }
            }
            val activeFilters = category != "All" || subcategory != "All types" || color != "All colors" || season != "All seasons" || occasion != "All occasions" || brand != "All brands" || favoritesOnly || search.isNotBlank()
            if (activeFilters) TextButton(onClick = { search = ""; category = "All"; subcategory = "All types"; color = "All colors"; season = "All seasons"; occasion = "All occasions"; brand = "All brands"; favoritesOnly = false }) { Text("Clear filters", color = CoutureAmber) }
            if (!syncMessage.isNullOrBlank()) Text(syncMessage!!, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 6.dp))
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FilterList, null, tint = CoutureAmber, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(if (items.isEmpty()) "Your wardrobe is waiting!" else "No clothing matches these filters", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(if (items.isEmpty()) "Add your first clothing item to get started." else "Clear filters or try another search.", color = TextSecondary, fontSize = 12.sp)
                    if (items.isEmpty()) TextButton(onClick = { editingItem = null; showAdd = true }) { Text("Add clothing", color = CoutureAmber) }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(155.dp), contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().height(((filtered.size + 1) / 2 * 245).coerceAtLeast(250).dp)
                ) {
                    items(filtered, key = { it.id }) { item -> WardrobeClothingCard(item, onClick = { selectedItem = item }, onFavorite = { viewModel.toggleFavorite(item) }) }
                }
            }
        }
    }
    if (showCatalog) {
        BuiltInCatalogDialog(
            wardrobe = items,
            onDismiss = { showCatalog = false },
            onAdd = { asset, name, selectedCategory, favorite ->
                val item = ClothingItem(
                    name = name,
                    category = selectedCategory,
                    subcategory = asset.subcategory,
                    color = asset.color,
                    style = asset.occasion,
                    season = asset.season,
                    formality = asset.occasion,
                    formalityScore = if (asset.occasion == "Work") 7 else 5,
                    fabric = "",
                    isFavorite = favorite,
                    imageRes = asset.imageRes,
                    imageUri = null,
                    pattern = asset.pattern,
                    occasion = asset.occasion,
                    description = "Bundled WearWise catalog style",
                    catalogAssetName = asset.assetName
                )
                viewModel.save(item) { }
            }
        )
    }
    if (showAdd || editingItem != null) {
        WardrobeItemDialog(
            item = editingItem,
            photoPath = selectedPhotoPath,
            onPhotoPath = { selectedPhotoPath = it },
            saving = saving,
            suggesting = suggesting,
            uploadProgress = uploadProgress,
            error = error,
            onDismiss = { showAdd = false; editingItem = null; selectedPhotoPath = null; viewModel.clearError() },
            onPickGallery = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onTakePhoto = {
                if (cameraPermission.status.isGranted) {
                    val (uri, _) = fileStore.createCameraOutputUri(); pendingCamera = uri; cameraPicker.launch(uri)
                } else cameraPermission.launchPermissionRequest()
            },
            onSuggest = { path, apply -> viewModel.suggestFromPhoto(path, apply) },
            onSave = { value -> viewModel.save(value, editingItem?.id) { if (it) { showAdd = false; editingItem = null; selectedPhotoPath = null } } }
        )
    }
    selectedItem?.let { item ->
        WardrobeDetailsDialog(
            item = item,
            onDismiss = { selectedItem = null },
            onFavorite = { viewModel.toggleFavorite(item) },
            onEdit = { selectedItem = null; editingItem = item; selectedPhotoPath = item.imageUri; viewModel.clearError() },
            onDelete = { viewModel.delete(item); selectedItem = null },
            onTryOn = { selectedItem = null; onUseInTryOn(item) }
        )
    }
}

@Composable
private fun WardrobeClothingCard(item: ClothingItem, onClick: () -> Unit, onFavorite: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.clip(RoundedCornerShape(18.dp)).background(CardElevated).border(1.dp, CardBorder, RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(175.dp).background(CardElevated)) {
            val image = item.imageUri ?: item.imageUrl ?: item.imageRes ?: item.catalogAssetName?.let {
                context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 }
            }
            if (image != null) AsyncImage(image, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(34.dp).clip(CircleShape).background(Color(0xCC101421))) {
                Icon(if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Toggle favorite", tint = if (item.isFavorite) Color(0xFFF16C85) else Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.category} · ${item.color}", color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WardrobeItemDialog(
    item: ClothingItem?, photoPath: String?, onPhotoPath: (String?) -> Unit,
    saving: Boolean, suggesting: Boolean, uploadProgress: Float?, error: String?, onDismiss: () -> Unit,
    onPickGallery: () -> Unit, onTakePhoto: () -> Unit,
    onSuggest: (String, (WardrobeSuggestion) -> Unit) -> Unit,
    onSave: (ClothingItem) -> Unit
) {
    var name by remember(item) { mutableStateOf(item?.name.orEmpty()) }
    var category by remember(item) { mutableStateOf(item?.category ?: "Tops") }
    var subcategory by remember(item) { mutableStateOf(item?.subcategory.orEmpty()) }
    var color by remember(item) { mutableStateOf(item?.color.orEmpty()) }
    var pattern by remember(item) { mutableStateOf(item?.pattern.orEmpty()) }
    var brand by remember(item) { mutableStateOf(item?.brand.orEmpty()) }
    var size by remember(item) { mutableStateOf(item?.size.orEmpty()) }
    var season by remember(item) { mutableStateOf(item?.season ?: "All Season") }
    var occasion by remember(item) { mutableStateOf(item?.occasion ?: "Everyday") }
    var description by remember(item) { mutableStateOf(item?.description.orEmpty()) }
    var validationError by remember(item) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val imageUri = if (photoPath == "__REMOVE__") null else photoPath ?: item?.imageUri
    val previewUri: Any? = imageUri ?: item?.imageUrl ?: item?.imageRes ?: item?.catalogAssetName?.let {
        context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add clothing" else "Edit clothing", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onTakePhoto, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = CardElevated)) { Icon(Icons.Default.CameraAlt, null); Text("Camera") }
                    Button(onClick = onPickGallery, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = CardElevated)) { Icon(Icons.Default.Image, null); Text("Gallery") }
                }
                if (previewUri != null) {
                    AsyncImage(previewUri, "Selected clothing photo", Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                    TextButton(onClick = { onPhotoPath("__REMOVE__") }) { Text("Remove photo", color = Color(0xFFF16C85)) }
                }
                if (imageUri != null) {
                TextButton(onClick = { onSuggest(imageUri!!) { s -> category = s.category; subcategory = s.subcategory; color = s.color; pattern = s.pattern; description = s.description } }, enabled = !suggesting) {
                        if (suggesting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Tune, null)
                        Text(if (suggesting) "Analyzing…" else "Suggest details with AI")
                    }
                }
                WardrobeField("Name *", name) { name = it }
                ChoiceMenu("Category: $category", WardrobeTaxonomy.categories.keys.toList(), category) { category = it; subcategory = "" }
                ChoiceMenu("Type: ${subcategory.ifBlank { "Select" }}", WardrobeTaxonomy.categories[category].orEmpty(), subcategory) { subcategory = it }
                WardrobeField("Color *", color) { color = it }
                WardrobeField("Pattern", pattern) { pattern = it }
                WardrobeField("Brand (optional)", brand) { brand = it }
                WardrobeField("Size (optional)", size) { size = it }
                ChoiceMenu("Season: $season", WardrobeTaxonomy.seasons, season) { season = it }
                ChoiceMenu("Occasion: $occasion", WardrobeTaxonomy.occasions, occasion) { occasion = it }
                WardrobeField("Description (optional)", description, singleLine = false) { description = it }
                if (!validationError.isNullOrBlank()) Text(validationError.orEmpty(), color = Color(0xFFFF8794), fontSize = 11.sp)
                if (!error.isNullOrBlank()) Text(error, color = Color(0xFFFF8794), fontSize = 11.sp)
                if (uploadProgress != null) {
                    Text("Uploading clothing photo ${(uploadProgress * 100).toInt()}%", color = CoutureAmber, fontSize = 11.sp)
                    androidx.compose.material3.LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                if (name.isBlank() || category.isBlank() || subcategory.isBlank() || color.isBlank()) {
                    validationError = "Name, category, clothing type, and color are required."
                    return@TextButton
                }
                validationError = null
                onSave((item ?: ClothingItem(name = name.trim(), category = category, color = color.trim(), style = occasion)).copy(
                    name = name.trim(), category = category, subcategory = subcategory, color = color.trim(), pattern = pattern.trim(), brand = brand.trim(), size = size.trim(),
                    season = season, occasion = occasion, description = description.trim(), style = occasion,
                    imageUri = imageUri, imageRes = if (photoPath == "__REMOVE__") null else if (imageUri == null) item?.imageRes else null,
                    imageUrl = if (photoPath == "__REMOVE__") null else item?.imageUrl,
                    imageStoragePath = if (photoPath == "__REMOVE__") null else item?.imageStoragePath,
                    remoteId = item?.remoteId, createdAt = item?.createdAt ?: System.currentTimeMillis()
                ))
            }) { Text(if (saving) "Saving…" else "Save clothing", color = CoutureAmber) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        containerColor = DarkNavyBg
    )
}

@Composable
private fun WardrobeDetailsDialog(item: ClothingItem, onDismiss: () -> Unit, onFavorite: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onTryOn: () -> Unit) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val image = item.imageUri ?: item.imageUrl ?: item.imageRes ?: item.catalogAssetName?.let {
                    context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 }
                }
                if (image != null) AsyncImage(image, item.name, Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                DetailRow("Category", listOf(item.category, item.subcategory).filter { it.isNotBlank() }.joinToString(" · "))
                DetailRow("Pattern", item.pattern.ifBlank { "—" })
                DetailRow("Color", item.color); DetailRow("Brand", item.brand.ifBlank { "—" }); DetailRow("Size", item.size.ifBlank { "—" })
                DetailRow("Season", item.season); DetailRow("Occasion", item.occasion); DetailRow("Description", item.description.ifBlank { "—" })
                DetailRow("Added", java.text.DateFormat.getDateInstance().format(java.util.Date(item.createdAt)))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onFavorite) { Text(if (item.isFavorite) "Remove favorite" else "Add favorite") }
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = Color(0xFFF16C85)) }
                }
                Button(onClick = onTryOn, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet)) { Text("Use in Virtual Try-On") }
                if (confirmDelete) AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text("Delete this item?") },
                    text = { Text("This removes it from your wardrobe and deletes its stored photo.") },
                    confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFF16C85)) } },
                    dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
                )
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = CoutureAmber) } }, containerColor = DarkNavyBg
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 12.sp); Text(value, color = TextPrimary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WardrobeField(label: String, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else 3,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = RoyalElectricViolet, unfocusedBorderColor = CardBorder, focusedContainerColor = CardElevated, unfocusedContainerColor = CardElevated))
}

@Composable
private fun StatPill(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(CardElevated).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), color = CoutureAmber, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(label, color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) RoyalElectricViolet else CardElevated).border(1.dp, if (selected) RoyalElectricViolet else CardBorder, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(text, color = if (selected) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun ChoiceMenu(label: String, choices: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(CardElevated).border(1.dp, CardBorder, RoundedCornerShape(18.dp)).clickable { expanded = true }.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

data class CatalogAsset(
    val assetName: String,
    val name: String,
    val category: String,
    val subcategory: String,
    val color: String,
    val occasion: String,
    val season: String,
    val pattern: String,
    val imageRes: Int?
)

@Composable
fun BuiltInCatalogDialog(
    wardrobe: List<ClothingItem>,
    onDismiss: () -> Unit,
    onAdd: (CatalogAsset, String, String, Boolean) -> Unit
) {
    var favorite by remember { mutableStateOf(false) }

    val sampleAssets = remember {
        listOf(
            CatalogAsset("oversized_tee", "Oversized Heavyweight Tee", "Tops", "T-shirts", "Matte Noir", "Everyday", "All Season", "Solid", com.example.presentation.common.WearWiseDrawables.ItemStreetTee),
            CatalogAsset("cargo_trousers", "Wide-Leg Cargo Trousers", "Bottoms", "Trousers", "Olive Drab", "Everyday", "All Season", "Solid", com.example.presentation.common.WearWiseDrawables.ItemCargoPants),
            CatalogAsset("tech_bomber", "Technical Bomber Jacket", "Outerwear", "Jackets", "Matte Charcoal", "Work", "Autumn", "Solid", com.example.presentation.common.WearWiseDrawables.ItemTechBomber),
            CatalogAsset("court_sneakers", "Leather Court Low-Tops", "Shoes", "Sneakers", "Triple White", "Everyday", "All Season", "Solid", com.example.presentation.common.WearWiseDrawables.ItemCourtSneakers)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Built-In Garment Catalog", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select a garment style to add to your closet:", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                sampleAssets.forEach { asset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardElevated)
                            .clickable {
                                onAdd(asset, asset.name, asset.category, favorite)
                                onDismiss()
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(asset.name, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(asset.category, color = CoutureAmber, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = DarkNavyBg
    )
}
