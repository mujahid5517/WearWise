package com.example.presentation.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ShoppingItem
import com.example.domain.repository.ShoppingRepository
import com.example.domain.usecase.GetWeatherUseCase
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.domain.usecase.UserProfileUseCase
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseTopBar
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(
    private val shoppingRepository: ShoppingRepository,
    manageWardrobeUseCase: ManageWardrobeUseCase,
    userProfileUseCase: UserProfileUseCase,
    getWeatherUseCase: GetWeatherUseCase
) : ViewModel() {

    val shoppingItems: StateFlow<List<ShoppingItem>> = shoppingRepository.observeShoppingItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(item: ShoppingItem) {
        viewModelScope.launch { shoppingRepository.addItem(item) }
    }

    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch { shoppingRepository.updateItem(item) }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch { shoppingRepository.deleteItem(id) }
    }
}

@Composable
fun EmptyShopScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .testTag("shop_screen_empty")
    )
}

@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel,
    onNavigateToProfile: () -> Unit
) {
    val items by viewModel.shoppingItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
    ) {
        WearWiseTopBar(
            title = "WEARWISE",
            subtitle = "SHOPPING WISHLIST",
            onAvatarClick = onNavigateToProfile
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Text(
                text = "Shopping & Wardrobe Expansion",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Items recommended to complete missing wardrobe gaps",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
                WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Your shopping wishlist is currently empty. Tap AI Advisor in Closet or Planner to find missing style pieces.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn {
                    items(items, key = { it.id }) { item ->
                        WearWiseCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Text(text = item.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "${item.category} • ${item.priority} Priority", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
