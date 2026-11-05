package com.example.presentation.home

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.domain.model.Outfit
import com.example.domain.model.UserProfile
import com.example.domain.model.previewCacheKey
import com.example.domain.repository.OutfitRepository
import com.example.domain.repository.OutfitFeedbackRepository
import com.example.domain.repository.OutfitFeedback
import com.example.domain.usecase.GetDailyRecommendationUseCase
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.domain.usecase.UserProfileUseCase
import com.example.domain.usecase.ExecuteTryOnUseCase
import com.example.presentation.common.GarmentSwapDialog
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseDrawables
import com.example.presentation.common.WearWiseOutfitCard
import com.example.presentation.common.WearWiseTopBar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CoutureAmber
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DecomposedGarmentSlot(
    val id: String,
    val name: String,
    val role: String,
    val spec: String,
    val imageRes: Int? = null,
    val imageUri: String? = null,
    val category: String,
    val imageUrl: String? = null,
    val wardrobeItemId: Long? = null
)

class HomeViewModel(
    private val getDailyRecommendationUseCase: GetDailyRecommendationUseCase,
    private val userProfileUseCase: UserProfileUseCase,
    private val manageWardrobeUseCase: ManageWardrobeUseCase,
    private val outfitRepository: OutfitRepository,
    private val feedbackRepository: OutfitFeedbackRepository,
    private val executeTryOnUseCase: ExecuteTryOnUseCase,
    private val isOnline: StateFlow<Boolean>
) : ViewModel() {

    private val _outfit = MutableStateFlow<Outfit?>(null)
    val outfit: StateFlow<Outfit?> = _outfit.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    private val _generatingAiLook = MutableStateFlow(false)
    val generatingAiLook: StateFlow<Boolean> = _generatingAiLook.asStateFlow()
    private val _generatingOutfitPreview = MutableStateFlow(false)
    val generatingOutfitPreview: StateFlow<Boolean> = _generatingOutfitPreview.asStateFlow()
    private val _outfitPreviewError = MutableStateFlow<String?>(null)
    val outfitPreviewError: StateFlow<String?> = _outfitPreviewError.asStateFlow()
    private val previewMutex = Mutex()
    private val attemptedPreviewKeys = mutableSetOf<Long>()
    val feedbackHistory: StateFlow<List<OutfitFeedback>> = feedbackRepository.observeFeedback()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _currentFeedback = MutableStateFlow<OutfitFeedback?>(null)
    val currentFeedback: StateFlow<OutfitFeedback?> = _currentFeedback.asStateFlow()

    val allWardrobeItems: StateFlow<List<ClothingItem>> = manageWardrobeUseCase.getAllItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _decomposedSlots = MutableStateFlow(
        listOf(
            DecomposedGarmentSlot(
                id = "slot_base",
                name = "White Oxford Shirt",
                role = "Primary",
                spec = "100% Breathable Cotton • Tailored",
                imageRes = WearWiseDrawables.ItemOxford,
                category = "Tops"
            ),
            DecomposedGarmentSlot(
                id = "slot_bottom",
                name = "Navy Chinos",
                role = "Bottom",
                spec = "Slim Tapered Cut • 98% Twill",
                imageRes = WearWiseDrawables.ItemChinos,
                category = "Bottoms"
            ),
            DecomposedGarmentSlot(
                id = "slot_outerwear",
                name = "Olive Knit Blazer",
                role = "Outerwear",
                spec = "Unlined Boiled Wool • Academic",
                imageRes = WearWiseDrawables.ItemBlazer,
                category = "Outerwear"
            ),
            DecomposedGarmentSlot(
                id = "slot_footwear",
                name = "Brown Burnished Loafers",
                role = "Footwear",
                spec = "Handcrafted Italian Calfskin",
                imageRes = WearWiseDrawables.ItemLoafers,
                category = "Shoes"
            ),
            DecomposedGarmentSlot(
                id = "slot_accents",
                name = "Minimalist Chrono",
                role = "Accents",
                spec = "Bauhaus Sapphire Dial",
                imageRes = WearWiseDrawables.ItemWatch,
                category = "Accessories"
            )
        )
    )
    val decomposedSlots: StateFlow<List<DecomposedGarmentSlot>> = _decomposedSlots.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            var previousOnline = isOnline.value
            isOnline.collect { online ->
                if (online && !previousOnline) _outfit.value?.let { ensureOutfitPreview(it) }
                previousOnline = online
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            manageWardrobeUseCase.getAllItems().collect {
                val recommendation = getDailyRecommendationUseCase()
                val cleanRecommendation = recommendation.copy(previewImageUri = null, previewImageRes = null)
                _outfit.value = cleanRecommendation
                _currentFeedback.value = feedbackRepository.getFeedback(recommendation)
                _decomposedSlots.value = cleanRecommendation.items.mapIndexed { index, item ->
                    DecomposedGarmentSlot(
                        id = "wardrobe_${item.id}_$index",
                        name = item.name,
                        role = item.category,
                        spec = listOf(item.subcategory, item.color, item.season).filter { it.isNotBlank() }.joinToString(" · "),
                        imageRes = item.imageRes,
                        imageUri = item.imageUri,
                        category = item.category,
                        imageUrl = item.imageUrl,
                        wardrobeItemId = item.id
                    )
                }
                ensureOutfitPreview(cleanRecommendation)
            }
        }
        viewModelScope.launch {
            userProfileUseCase.getProfile().collect { profile ->
                _userProfile.value = profile
            }
        }
    }

    fun submitFeedback(rating: Int? = _currentFeedback.value?.rating, reaction: String? = _currentFeedback.value?.reaction, comment: String = _currentFeedback.value?.comment.orEmpty()) {
        val current = _outfit.value ?: return
        viewModelScope.launch {
            feedbackRepository.saveFeedback(current, rating, reaction, comment)
            _currentFeedback.value = feedbackRepository.getFeedback(current)
        }
    }

    fun swapSlotItem(slotId: String, newItem: ClothingItem) {
        val oldItemId = _decomposedSlots.value.firstOrNull { it.id == slotId }?.wardrobeItemId
        val updated = _decomposedSlots.value.map { slot ->
            if (slot.id == slotId) {
                slot.copy(
                    name = newItem.name,
                    spec = "${newItem.fabric} • ${newItem.color}",
                    imageRes = newItem.imageRes,
                    imageUri = newItem.imageUri,
                    imageUrl = newItem.imageUrl,
                    wardrobeItemId = newItem.id
                )
            } else {
                slot
            }
        }
        _decomposedSlots.value = updated
        _outfit.value = _outfit.value?.copy(
            score = 99,
            items = _outfit.value?.items.orEmpty().filterNot { it.id == oldItemId } + newItem
        )
    }

    fun saveRecommendation(onSaved: () -> Unit) {
        val current = _outfit.value ?: return
        if (current.items.isEmpty()) return
        viewModelScope.launch {
            outfitRepository.saveOutfit(current.copy(id = 0L, title = "Saved: ${current.title}"))
            onSaved()
        }
    }

    fun generateAiRecommendation(onFinished: (Boolean, Boolean) -> Unit) {
        if (_generatingAiLook.value) return
        viewModelScope.launch {
            _generatingAiLook.value = true
            try {
                val suggestion = getDailyRecommendationUseCase.generateAiRecommendation()
                if (suggestion != null) {
                    _outfit.value = suggestion.copy(previewImageUri = null, previewImageRes = null)
                    _currentFeedback.value = feedbackRepository.getFeedback(suggestion)
                    _decomposedSlots.value = suggestion.items.mapIndexed { index, item ->
                        DecomposedGarmentSlot(
                            id = "wardrobe_${item.id}_$index", name = item.name, role = item.category,
                            spec = listOf(item.subcategory, item.color, item.season).filter { it.isNotBlank() }.joinToString(" · "),
                            imageRes = item.imageRes, imageUri = item.imageUri, imageUrl = item.imageUrl,
                            category = item.category, wardrobeItemId = item.id
                        )
                    }
                    ensureOutfitPreview(suggestion, force = true)
                }
                onFinished(suggestion != null, suggestion?.isAiGenerated == true)
            } catch (_: Exception) {
                onFinished(false, false)
            } finally {
                _generatingAiLook.value = false
            }
        }
    }

    fun retryOutfitPreview() {
        val current = _outfit.value ?: return
        viewModelScope.launch { ensureOutfitPreview(current, force = true) }
    }

    private suspend fun ensureOutfitPreview(outfit: Outfit, force: Boolean = false) {
        val cacheKey = outfit.previewCacheKey()
        previewMutex.withLock {
            val cached = executeTryOnUseCase.getCachedOutfitPreview(cacheKey)
            if (cached != null && (!force || !isOnline.value)) {
                if (_outfit.value?.previewCacheKey() == cacheKey) {
                    _outfit.value = _outfit.value?.copy(previewImageUri = cached, previewImageRes = null)
                }
                _outfitPreviewError.value = null
                return
            }
            if (!force && cacheKey in attemptedPreviewKeys) return
            if (outfit.items.isEmpty()) {
                _outfitPreviewError.value = "Add wardrobe items to generate a full-body look."
                return
            }
            if (!isOnline.value) {
                _outfitPreviewError.value = "Connect to the internet to generate a full-body try-on. Saved previews remain available offline."
                return
            }
            attemptedPreviewKeys.add(cacheKey)
            _generatingOutfitPreview.value = true
            _outfitPreviewError.value = null
            try {
                val modelImage = if (_userProfile.value?.gender.equals("female", ignoreCase = true)) {
                    WearWiseDrawables.TryOnCasualSmart
                } else {
                    WearWiseDrawables.TryOnModel
                }
                val imagePath = executeTryOnUseCase.generateOutfitPreview(outfit, modelImage)
                if (_outfit.value?.previewCacheKey() == cacheKey) {
                    _outfit.value = _outfit.value?.copy(previewImageUri = imagePath, previewImageRes = null)
                }
            } catch (error: Exception) {
                attemptedPreviewKeys.add(cacheKey)
                _outfitPreviewError.value = error.message?.takeIf(String::isNotBlank)
                    ?: "WearWise couldn't create this outfit image. Check the wardrobe photos and try again."
            } finally {
                _generatingOutfitPreview.value = false
            }
        }
    }

    fun shuffleRecommendation() {
        generateAiRecommendation { _, _ -> }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCloset: () -> Unit,
    onNavigateToAvatar: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
    val context = LocalContext.current
    val outfitState by viewModel.outfit.collectAsState()
    val generatingAiLook by viewModel.generatingAiLook.collectAsState()
    val generatingOutfitPreview by viewModel.generatingOutfitPreview.collectAsState()
    val outfitPreviewError by viewModel.outfitPreviewError.collectAsState()
    val userProfileState by viewModel.userProfile.collectAsState()
    val decomposedSlots by viewModel.decomposedSlots.collectAsState()
    val allWardrobeItems by viewModel.allWardrobeItems.collectAsState()

    var activeSwapSlot by remember { mutableStateOf<DecomposedGarmentSlot?>(null) }

    val userName = userProfileState?.firstName?.trim()?.takeIf(String::isNotEmpty) ?: "there"
    val outfit = outfitState ?: return
    val currentFeedback by viewModel.currentFeedback.collectAsState()
    var feedbackComment by remember(currentFeedback?.feedbackId, currentFeedback?.comment) { mutableStateOf(currentFeedback?.comment.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .testTag("home_screen")
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Greeting follows the currently signed-in user's profile.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CoutureAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI STYLIST ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = CoutureAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Good Morning,\n$userName",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CoutureAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${outfit.score}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Match",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weather & Schedule Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToPlanner() }
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = CoutureAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live weather in Planner", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RoyalElectricViolet, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Next: 2:00 PM Meeting", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Curated Outfit Card
            WearWiseOutfitCard(
                outfit = outfit,
                onWearThis = {
                    Toast.makeText(context, "Look #01 logged as today's ensemble!", Toast.LENGTH_SHORT).show()
                },
                onTryAnother = {
                    viewModel.shuffleRecommendation()
                },
                isGeneratingPreview = generatingOutfitPreview,
                previewError = outfitPreviewError,
                onGeneratePreview = viewModel::retryOutfitPreview
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Text("Helpful?", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(if (currentFeedback?.reaction == "LIKE") "♥ Liked" else "♡ Like", color = if (currentFeedback?.reaction == "LIKE") CoutureAmber else TextPrimary, modifier = Modifier.clickable { viewModel.submitFeedback(reaction = if (currentFeedback?.reaction == "LIKE") null else "LIKE") }.padding(8.dp))
                Text(if (currentFeedback?.reaction == "DISLIKE") "● Disliked" else "○ Dislike", color = if (currentFeedback?.reaction == "DISLIKE") RoyalElectricViolet else TextPrimary, modifier = Modifier.clickable { viewModel.submitFeedback(reaction = if (currentFeedback?.reaction == "DISLIKE") null else "DISLIKE") }.padding(8.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                (1..5).forEach { star ->
                    Text(if ((currentFeedback?.rating ?: 0) >= star) "★" else "☆", color = CoutureAmber, fontSize = 26.sp,
                        modifier = Modifier.clickable { viewModel.submitFeedback(rating = star) }.padding(horizontal = 5.dp, vertical = 3.dp))
                }
            }
            OutlinedTextField(value = feedbackComment, onValueChange = { feedbackComment = it.take(1000) },
                label = { Text("Add an optional note", color = TextSecondary, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), maxLines = 2)
            Text("SAVE FEEDBACK", color = CoutureAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End).clickable { viewModel.submitFeedback(comment = feedbackComment) }.padding(vertical = 5.dp))
            Text("VIEW FEEDBACK HISTORY & STYLE INSIGHTS", color = CoutureAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onNavigateToFeedback).padding(vertical = 8.dp))

            if (allWardrobeItems.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .clickable(enabled = !generatingAiLook) {
                            viewModel.generateAiRecommendation { success, aiEnhanced ->
                                val message = when {
                                    !success -> "Add clothing to your wardrobe to get a match."
                                    aiEnhanced -> "Gemini outfit match ready"
                                    else -> "Gemini is unavailable. Showing your offline wardrobe match."
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (generatingAiLook) "AI IS STYLING YOUR WARDROBE..." else "ASK AI TO STYLE MY WARDROBE", color = CoutureAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("When you request an AI look, clothing names and details are sent to Gemini.", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
            }

            if (outfit.items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.saveRecommendation {
                                Toast.makeText(context, "Wardrobe look saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAVE THIS WARDROBE LOOK", color = CoutureAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Garment Decomposition Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GARMENT DECOMPOSITION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = CoutureAmber
                )
                Text(
                    text = "${decomposedSlots.size} IN-CLOSET ITEMS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Garment Rows
            decomposedSlots.forEach { slot ->
                DecomposedGarmentItem(
                    slot = slot,
                    onSwap = {
                        activeSwapSlot = slot
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Garment Swap Dialog
    activeSwapSlot?.let { slot ->
        GarmentSwapDialog(
            currentSlotTitle = slot.role,
            initialCategory = slot.category,
            allGarments = allWardrobeItems,
            onGarmentSelected = { newItem ->
                viewModel.swapSlotItem(slot.id, newItem)
                Toast.makeText(context, "Swapped with ${newItem.name}!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { activeSwapSlot = null }
        )
    }
}

@Composable
private fun DecomposedGarmentItem(
    slot: DecomposedGarmentSlot,
    onSwap: () -> Unit
) {
    WearWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardElevated)
                ) {
                    if (slot.imageRes != null) {
                        Image(
                            painter = painterResource(id = slot.imageRes),
                            contentDescription = slot.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!slot.imageUri.isNullOrEmpty() || !slot.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = slot.imageUri ?: slot.imageUrl,
                            contentDescription = slot.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = slot.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CardBorder)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = slot.role,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalElectricViolet
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = slot.spec,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            IconButton(onClick = onSwap) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap garment",
                    tint = CoutureAmber,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
