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
import com.example.domain.usecase.GetDailyRecommendationUseCase
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.domain.usecase.UserProfileUseCase
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

data class DecomposedGarmentSlot(
    val id: String,
    val name: String,
    val role: String,
    val spec: String,
    val imageRes: Int? = null,
    val imageUri: String? = null,
    val category: String
)

class HomeViewModel(
    private val getDailyRecommendationUseCase: GetDailyRecommendationUseCase,
    private val userProfileUseCase: UserProfileUseCase,
    private val manageWardrobeUseCase: ManageWardrobeUseCase
) : ViewModel() {

    private val _outfit = MutableStateFlow<Outfit?>(null)
    val outfit: StateFlow<Outfit?> = _outfit.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

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
    }

    private fun loadData() {
        viewModelScope.launch {
            _outfit.value = getDailyRecommendationUseCase()
            userProfileUseCase.getProfile().collect { profile ->
                _userProfile.value = profile
            }
        }
    }

    fun swapSlotItem(slotId: String, newItem: ClothingItem) {
        val updated = _decomposedSlots.value.map { slot ->
            if (slot.id == slotId) {
                slot.copy(
                    name = newItem.name,
                    spec = "${newItem.fabric} • ${newItem.color}",
                    imageRes = newItem.imageRes,
                    imageUri = newItem.imageUri
                )
            } else {
                slot
            }
        }
        _decomposedSlots.value = updated
        _outfit.value = _outfit.value?.copy(score = 99)
    }

    fun shuffleRecommendation() {
        viewModelScope.launch {
            val current = _outfit.value
            if (current != null) {
                val newScore = if (current.score == 98) 96 else 98
                val newTitle = if (current.title == "The Academic Tailor") "Modern Colloquium Core" else "The Academic Tailor"
                val newRationale = if (newTitle == "Modern Colloquium Core") {
                    "Layered charcoal merino knit paired with structured wool and tailored chinos. Ideal for classroom transitions with optimal thermal comfort."
                } else {
                    "Tailored specifically for your lecturer schedule. The 100% Oxford cotton weave ensures all-day breathability during the Faculty Meeting, while the structured navy chinos project relaxed academic authority at the 2:00 PM Colloquium."
                }
                _outfit.value = current.copy(
                    title = newTitle,
                    score = newScore,
                    rationale = newRationale
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCloset: () -> Unit,
    onNavigateToAvatar: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val outfitState by viewModel.outfit.collectAsState()
    val userProfileState by viewModel.userProfile.collectAsState()
    val decomposedSlots by viewModel.decomposedSlots.collectAsState()
    val allWardrobeItems by viewModel.allWardrobeItems.collectAsState()

    var activeSwapSlot by remember { mutableStateOf<DecomposedGarmentSlot?>(null) }

    val userName = userProfileState?.firstName ?: "Yared"
    val outfit = outfitState ?: return

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
            // Header: Good Morning, Yared
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
                            text = "NEURAL STYLIST ACTIVE",
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
                                color = Color.White
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = CoutureAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sunny 21°C • Campus", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
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
                }
            )

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
                        .background(Color(0xFF1B2032))
                ) {
                    if (slot.imageRes != null) {
                        Image(
                            painter = painterResource(id = slot.imageRes),
                            contentDescription = slot.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!slot.imageUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = slot.imageUri,
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
                                .background(Color(0xFF22283A))
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
