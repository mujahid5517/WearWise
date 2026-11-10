package com.example.presentation.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ShoppingChatMessage
import com.example.domain.model.ShoppingItem
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseTopBar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardElevated
import com.example.ui.theme.ChipActiveBg
import com.example.ui.theme.ChipActiveBorder
import com.example.ui.theme.ChipInactiveBg
import com.example.ui.theme.ChipInactiveBorder
import com.example.ui.theme.CoutureAmber
import com.example.ui.theme.CoutureEmerald
import com.example.ui.theme.CoutureRose
import com.example.ui.theme.DangerBorder
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ShoppingScreen(
    viewModel: ShoppingViewModel? = null,
    onNavigateToProfile: () -> Unit = {},
    showCloseButton: Boolean = false,
    onNavigateBack: () -> Unit = {}
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value ?: ShoppingUiState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .navigationBarsPadding()
            .imePadding()
            .testTag("shopping_screen_root")
    ) {
        // Custom Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RoyalElectricViolet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Stylist",
                        tint = RoyalElectricViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "YOUR STYLE ASSISTANT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CoutureEmerald)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${uiState.wardrobeCount} Closet Items Connected",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Row {
                if (viewModel != null) {
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (showCloseButton || viewModel != null) {
                    IconButton(
                        onClick = if (showCloseButton) onNavigateBack else onNavigateToProfile,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (showCloseButton) Icons.Default.Close else Icons.Default.Person,
                            contentDescription = if (showCloseButton) "Close assistant" else "Profile",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Occasion Context Chips Bar
        val occasions = listOf("Everyday", "Work", "Formal", "Party", "Date Night", "Travel", "Sport")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.weatherDescription?.let { weatherText ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardElevated)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = weatherText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CoutureAmber
                    )
                }
            }

            occasions.forEach { item ->
                val selected = uiState.selectedOccasion == item
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) ChipActiveBg else ChipInactiveBg)
                        .border(
                            1.dp,
                            if (selected) ChipActiveBorder else ChipInactiveBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = viewModel != null) { viewModel?.selectOccasion(item) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) RoyalElectricViolet else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            uiState.syncMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = CoutureAmber,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().testTag("shopping_sync_status")
                    )
                }
            }
            items(uiState.messages) { message ->
                if (message.isUser) {
                    UserMessageBubble(message = message)
                } else {
                    AssistantMessageBubble(message = message)
                }
            }

            // Quick Prompt Suggestions (shown when chat is new)
            if (viewModel != null && uiState.messages.size <= 2 && !uiState.isLoading) {
                item {
                    QuickPromptsSection(onSelectPrompt = { prompt ->
                        viewModel?.selectQuickPrompt(prompt)
                    })
                }
            }

            // Loading Indicator Bubble
            if (uiState.isLoading) {
                item {
                    LoadingBubble()
                }
            }

            // Error Card
            if (uiState.errorMessage != null) {
                item {
                    ErrorCard(
                        errorMessage = uiState.errorMessage,
                        onRetry = { viewModel?.retryLastMessage() },
                        onNavigateToProfile = onNavigateToProfile,
                        onDismiss = { viewModel?.dismissError() }
                    )
                }
            }
        }

        // Bottom Chat Input Bar
        if (viewModel != null) {
            SurfaceInputBar(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                isLoading = uiState.isLoading,
                onSend = {
                    if (inputText.isNotBlank() && !uiState.isLoading) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun UserMessageBubble(message: ShoppingChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 4.dp,
                        bottomEnd = 18.dp,
                        bottomStart = 18.dp
                    )
                )
                .background(RoyalElectricViolet)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(message: ShoppingChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(RoyalElectricViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI",
                tint = RoyalElectricViolet,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 18.dp,
                        bottomEnd = 18.dp,
                        bottomStart = 18.dp
                    )
                )
                .background(CardElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.text,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(RoyalElectricViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Thinking",
                tint = RoyalElectricViolet,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CardElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = RoyalElectricViolet,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Checking your saved wardrobe...",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
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
fun ShoppingWishlistScreen(
    viewModel: ShoppingViewModel,
    onNavigateToProfile: () -> Unit,
    onOpenAssistant: () -> Unit = {}
) {
    val items by viewModel.shoppingItems.collectAsState()
    val purchaseHistory by viewModel.purchaseHistory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf("Newest") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var purchasingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showingEditor by remember { mutableStateOf(false) }
    var showingBudgetEditor by remember { mutableStateOf(false) }
    var showingPurchaseHistory by remember { mutableStateOf(false) }

    val visibleItems = filterShoppingItems(
        items = items,
        query = query,
        category = category,
        statusFilter = statusFilter,
        favoritesOnly = favoritesOnly,
        sortOrder = sortOrder
    )
    val purchasedItems = purchaseHistory
    val expenseSummary = summarizePurchases(purchasedItems)
    val spent = purchasedItems.filter { it.currency.equals(uiState.currency, true) }
        .sumOf { (it.actualPrice ?: it.estimatedPrice).coerceAtLeast(0.0) * it.quantity.coerceAtLeast(1) }
    val categoryTotals = purchasedItems.filter { it.currency.equals(uiState.currency, true) }
        .groupBy { it.category.ifBlank { "Other" } }
        .mapValues { (_, rows) ->
            rows.sumOf { (it.actualPrice ?: it.estimatedPrice).coerceAtLeast(0.0) * it.quantity.coerceAtLeast(1) }
        }.toSortedMap()
    val otherCurrencyTotals = purchasedItems.filterNot { it.currency.equals(uiState.currency, true) }
        .groupBy { it.currency }
        .mapValues { (_, rows) ->
            rows.sumOf { (it.actualPrice ?: it.estimatedPrice).coerceAtLeast(0.0) * it.quantity.coerceAtLeast(1) }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
    ) {
        WearWiseTopBar(
            title = "WEARWISE",
            subtitle = "STYLE · SHOP",
            onAvatarClick = onNavigateToProfile
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                ShoppingHero(onOpenAssistant = onOpenAssistant)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Discover Your Style", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Your wardrobe-aware shopping space", fontSize = 13.sp, color = TextSecondary)
                    }
                    IconButton(onClick = { editingItem = null; showingEditor = true }) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = "Add shopping item", tint = RoyalElectricViolet)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search your saved items") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("shopping_search"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            item {
                ShoppingBudgetCard(
                    currency = uiState.currency,
                    budget = uiState.budget,
                    spent = spent,
                    purchaseCount = expenseSummary.purchasedItemCount,
                    categoryTotals = categoryTotals,
                    otherCurrencyTotals = otherCurrencyTotals,
                    purchases = purchasedItems,
                    historyExpanded = showingPurchaseHistory,
                    isSaving = uiState.isSavingBudget,
                    onConfigureBudget = { showingBudgetEditor = true },
                    onToggleHistory = { showingPurchaseHistory = !showingPurchaseHistory }
                )
            }
            uiState.syncMessage?.let { message ->
                item { Text(message, color = CoutureAmber, fontSize = 12.sp, modifier = Modifier.testTag("shopping_sync_status")) }
            }
            item {
                WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Recommended for You", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Wardrobe gap discovery · personalized ideas, not store products.", color = TextSecondary, fontSize = 12.sp)
                            }
                            TextButton(onClick = viewModel::refreshRecommendations) { Text("Refresh") }
                        }
                        val analysis = uiState.analysis
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            listOf("Everyday", "Work", "Formal", "Party", "Travel", "Sport").forEach { occasion ->
                                FilterChip(
                                    selected = uiState.selectedOccasion == occasion,
                                    onClick = { viewModel.selectOccasion(occasion) },
                                    label = { Text(occasion) }
                                )
                            }
                        }
                        uiState.weatherDescription?.let {
                            Text("Weather context · $it", color = CoutureAmber, fontSize = 11.sp)
                        }
                        Text(analysis?.summary ?: "Analyzing your saved wardrobe…", color = TextSecondary, fontSize = 12.sp)
                        analysis?.recommendations.orEmpty().forEach { recommendation ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(recommendation.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(recommendation.why, color = TextSecondary, fontSize = 11.sp)
                                    if (recommendation.budgetTarget > 0) {
                                        Text("Budget allocation: ${money(uiState.currency, recommendation.budgetTarget)} · not a product price", color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                                TextButton(onClick = {
                                    viewModel.addItem(
                                        ShoppingItem(
                                            name = recommendation.name,
                                            category = recommendation.category,
                                            reason = recommendation.why,
                                            priority = recommendation.priority,
                                            estimatedPrice = 0.0,
                                            currency = uiState.currency
                                        )
                                    )
                                }) { Text("Add to wishlist") }
                            }
                        }
                        if (analysis?.recommendations.isNullOrEmpty()) {
                            Text("No category gaps to show yet; add wardrobe items or select Refresh.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Categories", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categoryOptions = listOf("All", "Clothing", "Shoes", "Accessories", "Bags", "Watches", "Jewelry", "Other") +
                            items.map { it.category }.filter { it.isNotBlank() }
                                .distinct().filterNot { existing -> listOf("All", "Clothing", "Shoes", "Accessories", "Bags", "Watches", "Jewelry", "Other").any { it.equals(existing, true) } }
                        categoryOptions.distinctBy { it.lowercase() }.forEach { option ->
                            FilterChip(
                                selected = category.equals(option, true),
                                onClick = { category = option },
                                label = { Text(option) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All", "Wishlist", "Purchased").forEach { option ->
                            FilterChip(
                                selected = statusFilter == option,
                                onClick = { statusFilter = option },
                                label = { Text(option) }
                            )
                        }
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            label = { Text(if (favoritesOnly) "♥ Favorites" else "♡ Favorites") }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box {
                            TextButton(onClick = { sortMenuExpanded = true }) { Text("Sort: $sortOrder") }
                            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                listOf("Newest", "Oldest", "Name", "Price: low to high", "Price: high to low").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = { sortOrder = option; sortMenuExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Start your wishlist", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Save custom ideas or add wardrobe-gap suggestions. A live product catalog is not connected.", color = TextSecondary, fontSize = 13.sp)
                            OutlinedButton(onClick = { editingItem = null; showingEditor = true }) { Text("Add custom item") }
                        }
                    }
                }
            } else if (visibleItems.isEmpty()) {
                item { Text("No saved items match your search and filters.", color = TextSecondary, modifier = Modifier.padding(vertical = 18.dp)) }
            } else {
                item {
                    Text(
                        "Your saved list · ${visibleItems.size}",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(visibleItems.chunked(2), key = { row -> row.joinToString("-") { it.id.toString() } }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { item ->
                            ShoppingProductCard(
                                item = item,
                                modifier = Modifier.weight(1f),
                                onEdit = { editingItem = item; showingEditor = true },
                                onFavorite = { viewModel.setFavorite(item.id, !item.favorite) },
                                onPurchase = {
                                    if (item.status.equals("PURCHASED", true)) viewModel.markNeeded(item)
                                    else purchasingItem = item
                                },
                                onDelete = { viewModel.deleteItem(item.id) },
                                onQuantityChange = { newQuantity -> viewModel.updateItem(item.copy(quantity = newQuantity.coerceAtLeast(1))) }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showingEditor) {
        ShoppingItemEditor(
            initial = editingItem,
            defaultCurrency = uiState.currency,
            onDismiss = { showingEditor = false },
            onSave = { item ->
                if (editingItem == null) viewModel.addItem(item)
                else viewModel.updateItem(item)
                showingEditor = false
            }
        )
    }
    if (showingBudgetEditor) {
        ShoppingBudgetEditor(
            currentAmount = uiState.budget,
            currency = uiState.currency,
            onDismiss = { showingBudgetEditor = false },
            onSave = { amount, currency ->
                viewModel.saveBudget(amount, currency)
                showingBudgetEditor = false
            }
        )
    }
    purchasingItem?.let { item ->
        PurchasePriceEditor(
            item = item,
            onDismiss = { purchasingItem = null },
            onSave = { actualPrice ->
                viewModel.markPurchased(item, actualPrice)
                purchasingItem = null
            }
        )
    }
}

internal fun filterShoppingItems(
    items: List<ShoppingItem>,
    query: String,
    category: String,
    statusFilter: String,
    favoritesOnly: Boolean,
    sortOrder: String
): List<ShoppingItem> = items
        .filter { !favoritesOnly || it.favorite }
        .filter { matchesShoppingCategory(category, it.category) }
        .filter {
            when (statusFilter) {
                "Wishlist" -> !it.status.equals("PURCHASED", true)
                "Purchased" -> it.status.equals("PURCHASED", true)
                else -> true
            }
        }
        .filter {
            query.isBlank() || it.name.contains(query, true) ||
                it.category.contains(query, true) || it.reason.contains(query, true)
        }
        .let { list ->
            when (sortOrder) {
                "Name" -> list.sortedBy { it.name.lowercase() }
                "Oldest" -> list.sortedBy { it.createdAt }
                "Price: low to high" -> list.sortedBy { it.estimatedPrice }
                "Price: high to low" -> list.sortedByDescending { it.estimatedPrice }
                else -> list.sortedByDescending { it.createdAt }
            }
        }
private fun matchesShoppingCategory(filter: String, category: String): Boolean {
    if (filter.equals("All", true)) return true
    val value = category.lowercase()
    return when (filter.lowercase()) {
        "clothing" -> listOf("top", "bottom", "cloth", "dress", "shirt", "outerwear", "knit").any(value::contains)
        "shoes" -> listOf("shoe", "boot", "sneaker", "sandal", "footwear").any(value::contains)
        "accessories" -> listOf("accessor", "bag", "watch", "jewel", "belt", "hat").any(value::contains)
        "bags" -> value.contains("bag")
        "watches" -> value.contains("watch")
        "jewelry" -> value.contains("jewel")
        "other" -> listOf("top", "bottom", "cloth", "dress", "shirt", "outerwear", "knit", "shoe", "boot", "sneaker", "sandal", "footwear", "accessor", "bag", "watch", "jewel", "belt", "hat")
            .none(value::contains)
        else -> category.equals(filter, true)
    }
}

@Composable
private fun ShoppingHero(onOpenAssistant: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3B236E), Color(0xFF191B34), CardElevated)))
            .border(1.dp, RoyalElectricViolet.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CoutureAmber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("YOUR PERSONAL STYLE EDIT", color = CoutureAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            }
            Text("Find Your\nPerfect Style", color = TextPrimary, fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
            Text(
                "Thoughtful wardrobe-gap ideas, grounded in what you already own.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Button(
                onClick = onOpenAssistant,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Ask your style assistant")
            }
            Text("Saved ideas only · no live store catalog connected", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ShoppingBudgetCard(
    currency: String,
    budget: Double,
    spent: Double,
    purchaseCount: Int,
    categoryTotals: Map<String, Double>,
    otherCurrencyTotals: Map<String, Double>,
    purchases: List<ShoppingItem>,
    historyExpanded: Boolean,
    isSaving: Boolean,
    onConfigureBudget: () -> Unit,
    onToggleHistory: () -> Unit
) {
    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Shopping budget", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(if (budget > 0) "Your configured limit" else "Set a limit to track progress", color = TextSecondary, fontSize = 12.sp)
                }
                TextButton(onClick = onConfigureBudget, enabled = !isSaving) {
                    Text(if (isSaving) "Saving…" else if (budget > 0) "Edit" else "Set budget")
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                BudgetMetric("BUDGET", if (budget > 0) money(currency, budget) else "Not set")
                BudgetMetric("SPENT", money(currency, spent))
                BudgetMetric("REMAINING", if (budget > 0) money(currency, (budget - spent).coerceAtLeast(0.0)) else "—")
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(CardBorder)
            ) {
                if (budget > 0) {
                    Box(
                        modifier = Modifier.fillMaxWidth((spent / budget).coerceIn(0.0, 1.0).toFloat())
                            .height(7.dp).clip(CircleShape)
                            .background(if (spent > budget) CoutureRose else RoyalElectricViolet)
                    )
                }
            }
            if (budget > 0 && spent > budget) {
                Text("You are over budget by ${money(currency, spent - budget)}.", color = CoutureRose, fontSize = 11.sp)
            }
            categoryTotals.forEach { (name, total) ->
                Text("$name  ·  ${money(currency, total)}", color = TextSecondary, fontSize = 11.sp)
            }
            if (otherCurrencyTotals.isNotEmpty()) {
                Text(
                    "Other currencies: " + otherCurrencyTotals.entries.joinToString(" · ") { (code, total) -> money(code, total) },
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Purchase history · $purchaseCount", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                TextButton(onClick = onToggleHistory, enabled = purchaseCount > 0) {
                    Text(if (historyExpanded) "Hide" else "View")
                }
            }
            if (historyExpanded && purchases.isNotEmpty()) {
                purchases.forEach { purchase ->
                    val date = purchase.purchasedAt?.let {
                        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                    } ?: "Date not recorded"
                    Text(
                        "${purchase.name} · qty ${purchase.quantity} · $date · ${money(purchase.currency, (purchase.actualPrice ?: purchase.estimatedPrice) * purchase.quantity)}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            } else if (purchaseCount == 0) {
                Text("Record a purchase from your wishlist to start tracking actual spending.", color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun BudgetMetric(label: String, value: String) {
    Column(modifier = Modifier.padding(end = 4.dp)) {
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShoppingProductCard(
    item: ShoppingItem,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onPurchase: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    WearWiseCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(0.95f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(RoyalElectricViolet.copy(alpha = 0.28f), CardElevated, CardBorder))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Checkroom,
                    contentDescription = if (item.reason.startsWith("Your closet", true)) "Wardrobe gap illustration" else "Saved custom idea illustration",
                    tint = RoyalElectricViolet.copy(alpha = 0.8f),
                    modifier = Modifier.size(46.dp)
                )
                Text(
                    if (item.reason.startsWith("Your closet", true)) "WARDROBE GAP" else "SAVED IDEA",
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)).background(DarkNavyBg.copy(alpha = 0.88f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    color = TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        imageVector = if (item.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (item.favorite) "Remove from favorites" else "Add to favorites",
                        tint = if (item.favorite) CoutureRose else TextPrimary
                    )
                }
            }
            Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
            Text(item.category, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
            Text(
                if (item.status.equals("PURCHASED", true)) money(item.currency, item.actualPrice ?: item.estimatedPrice)
                else if (item.estimatedPrice > 0) money(item.currency, item.estimatedPrice) else "No price set",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onQuantityChange(item.quantity - 1) }, enabled = item.quantity > 1, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("−") }
                    Text("${item.quantity}", color = TextPrimary, fontSize = 12.sp)
                    TextButton(onClick = { onQuantityChange(item.quantity + 1) }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("+") }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove saved item", tint = TextMuted, modifier = Modifier.size(17.dp))
                }
            }
            TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 2.dp)) {
                Text("View details", fontSize = 11.sp)
            }
            OutlinedButton(onClick = onPurchase, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 3.dp)) {
                Text(if (item.status.equals("PURCHASED", true)) "Undo purchase" else "Record purchase", fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PurchasePriceEditor(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var actualPrice by remember(item.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record purchase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the actual unit price paid for ${item.name}. The estimate is ${money(item.currency, item.estimatedPrice)}.", color = TextSecondary)
                OutlinedTextField(
                    actualPrice,
                    { actualPrice = it },
                    label = { Text("Actual unit price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text("Only the value you enter is counted as actual spending.", color = TextMuted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(
                enabled = actualPrice.toDoubleOrNull()?.let { it >= 0.0 } == true,
                onClick = { actualPrice.toDoubleOrNull()?.let(onSave) }
            ) { Text("Save purchase") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ShoppingItemEditor(
    initial: ShoppingItem?,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Clothing") }
    var estimate by remember(initial) { mutableStateOf(initial?.estimatedPrice?.takeIf { it > 0 }?.toString().orEmpty()) }
    var actual by remember(initial) { mutableStateOf(initial?.actualPrice?.toString().orEmpty()) }
    var quantity by remember(initial) { mutableStateOf((initial?.quantity ?: 1).toString()) }
    var reason by remember(initial) { mutableStateOf(initial?.reason.orEmpty()) }
    var priority by remember(initial) { mutableStateOf(initial?.priority ?: "MEDIUM") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add custom list item" else "Item details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Item name") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(estimate, { estimate = it }, label = { Text("Estimated unit price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                if (initial?.status.equals("PURCHASED", true)) {
                    OutlinedTextField(actual, { actual = it }, label = { Text("Actual unit price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(priority, { priority = it }, label = { Text("Priority") }, singleLine = true)
                OutlinedTextField(reason, { reason = it }, label = { Text("Why it's on your list") }, minLines = 2)
                Text("Custom entry only. No store lookup or purchase link is generated.", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && category.isNotBlank(),
                onClick = {
                    val estimateValue = estimate.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    onSave(
                        (initial ?: ShoppingItem(name = name.trim(), category = category.trim())).copy(
                            name = name.trim(),
                            category = category.trim(),
                            estimatedPrice = estimateValue,
                            actualPrice = actual.toDoubleOrNull()?.coerceAtLeast(0.0) ?: initial?.actualPrice,
                            quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            reason = reason.trim(),
                            priority = priority.trim().ifBlank { "MEDIUM" },
                            currency = initial?.currency ?: defaultCurrency
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ShoppingBudgetEditor(
    currentAmount: Double,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amount by remember(currentAmount) { mutableStateOf(if (currentAmount > 0) currentAmount.toString() else "") }
    var currencyText by remember(currency) { mutableStateOf(currency) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure shopping budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Budget amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(currencyText, { currencyText = it.uppercase().take(3) }, label = { Text("Currency code") }, singleLine = true)
                Text("Budget is separate from actual purchased-item totals.", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount.toDoubleOrNull() != null && currencyText.length == 3,
                onClick = { onSave(amount.toDoubleOrNull()!!.coerceAtLeast(0.0), currencyText); }
            ) { Text("Save budget") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun money(currency: String, amount: Double): String =
    "$currency ${"%.2f".format(java.util.Locale.US, amount)}"

@Composable
private fun QuickPromptsSection(onSelectPrompt: (String) -> Unit) {
    val prompts = listOf(
        "👗 What should I wear for work today?",
        "👔 Build 3 outfit combinations from my closet",
        "⛅ Suggest an outfit for today's weather",
        "🛍️ What key clothing pieces am I missing?",
        "🎨 Which colors match my style preferences?"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Suggestions",
                tint = CoutureAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SUGGESTED STYLING QUESTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextMuted
            )
        }

        prompts.forEach { prompt ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .clickable { onSelectPrompt(prompt) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = prompt,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    val isKeyError = errorMessage.contains("API key", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DangerContainer)
            .border(1.dp, DangerBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = CoutureRose,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Assistant Error",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoutureRose
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isKeyError) {
                    Button(
                        onClick = onNavigateToProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Configure Key",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configure Key", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", fontSize = 12.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun SurfaceInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkNavyBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        text = "Ask WearWise AI styling questions...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4,
                singleLine = false
            )

            val canSend = inputText.isNotBlank() && !isLoading
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) RoyalElectricViolet else CardBorder)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = if (canSend) Color.White else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
