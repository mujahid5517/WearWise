package com.example.presentation.shopping

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
                        text = "AI FASHION ASSISTANT",
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
                    text = "WearWise AI is analyzing your wardrobe...",
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
    onNavigateToProfile: () -> Unit
) {
    val items by viewModel.shoppingItems.collectAsState()
    val purchaseHistory by viewModel.purchaseHistory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("Newest") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var purchasingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showingEditor by remember { mutableStateOf(false) }
    var showingBudgetEditor by remember { mutableStateOf(false) }

    val visibleItems = items
        .filter { !favoritesOnly || it.favorite }
        .filter { category == "All" || it.category.equals(category, true) }
        .filter {
            query.isBlank() || it.name.contains(query, true) ||
                it.category.contains(query, true) || it.reason.contains(query, true)
        }
        .let { list ->
            when (sortOrder) {
                "Name" -> list.sortedBy { it.name.lowercase() }
                "Price: low to high" -> list.sortedBy { it.estimatedPrice }
                "Price: high to low" -> list.sortedByDescending { it.estimatedPrice }
                "Priority" -> list.sortedBy { when (it.priority.uppercase()) { "HIGH" -> 0; "MEDIUM" -> 1; else -> 2 } }
                else -> list.sortedByDescending { it.createdAt }
            }
        }
    val purchasedItems = purchaseHistory
    val expenseSummary = summarizePurchases(purchasedItems)
    val spent = expenseSummary.totalSpent
    val categoryTotals = expenseSummary.categoryTotals

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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shopping & Wardrobe Expansion", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Your saved list and custom catalog. No live store catalog is connected.", fontSize = 12.sp, color = TextSecondary)
                    }
                    Button(onClick = { editingItem = null; showingEditor = true }) { Text("Add item") }
                }
            }
            item {
                WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Budget & actual spending", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            if (uiState.budget > 0) "Configured budget: ${money(uiState.currency, uiState.budget)}"
                            else "Budget not set",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            if (uiState.budget > 0) {
                                "Recorded purchases: ${money(uiState.currency, spent)} · Remaining: ${money(uiState.currency, (uiState.budget - spent).coerceAtLeast(0.0))}"
                            } else {
                                "Recorded purchases: ${money(uiState.currency, spent)} · Remaining: — (set a budget)"
                            },
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        TextButton(onClick = { showingBudgetEditor = true }) { Text("Configure budget") }
                        if (expenseSummary.purchasedItemCount > 0) {
                            Text("Purchase history (${expenseSummary.purchasedItemCount} items)", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            categoryTotals.forEach { (name, total) ->
                                Text("$name: ${money(uiState.currency, total)}", color = TextSecondary, fontSize = 12.sp)
                            }
                            purchasedItems.forEach { purchase ->
                                val purchaseDate = purchase.purchasedAt?.let {
                                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it))
                                } ?: "Date not recorded"
                                Text(
                                    "${purchase.name} · ${purchase.category} · qty ${purchase.quantity} · $purchaseDate · ${money(purchase.currency, (purchase.actualPrice ?: purchase.estimatedPrice) * purchase.quantity)}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Text("Purchase history is empty. Mark a list item purchased to track actual expense.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            uiState.syncMessage?.let { message ->
                item { Text(message, color = CoutureAmber, fontSize = 12.sp, modifier = Modifier.testTag("shopping_sync_status")) }
            }
            item {
                WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Wardrobe gap discovery", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Suggestions use your saved closet and profile; they are not products.", color = TextSecondary, fontSize = 12.sp)
                            }
                            TextButton(onClick = viewModel::refreshRecommendations) { Text("Refresh") }
                        }
                        val analysis = uiState.analysis
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
                                }) { Text("Add gap") }
                            }
                        }
                        if (analysis?.recommendations.isNullOrEmpty()) {
                            Text("No category gaps to show yet; add wardrobe items or select Refresh.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search saved items") },
                    modifier = Modifier.fillMaxWidth().testTag("shopping_search"),
                    singleLine = true
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val categories = listOf("All") + items.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                    Row(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { option ->
                            FilterChip(
                                selected = category.equals(option, true),
                                onClick = { category = option },
                                label = { Text(option) }
                            )
                        }
                    }
                    TextButton(onClick = { favoritesOnly = !favoritesOnly }) {
                        Text(if (favoritesOnly) "★ Favorites" else "☆ Favorites")
                    }
                    Box {
                        TextButton(onClick = { sortMenuExpanded = true }) { Text("Sort: $sortOrder") }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            listOf("Newest", "Name", "Priority", "Price: low to high", "Price: high to low").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { sortOrder = option; sortMenuExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Your shopping list is empty.", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Add a custom item or add a wardrobe-gap suggestion. Products and purchase links are not fabricated.", color = TextSecondary, fontSize = 13.sp)
                            OutlinedButton(onClick = { editingItem = null; showingEditor = true }) { Text("Add custom item") }
                        }
                    }
                }
            } else if (visibleItems.isEmpty()) {
                item { Text("No saved items match these filters.", color = TextSecondary, modifier = Modifier.padding(vertical = 18.dp)) }
            } else {
                items(visibleItems, key = { it.id }) { item ->
                    ShoppingListItemCard(
                        item = item,
                        currency = uiState.currency,
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

@Composable
private fun ShoppingListItemCard(
    item: ShoppingItem,
    currency: String,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onPurchase: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    WearWiseCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${item.category} · ${item.priority} priority · ${item.status.lowercase()}", color = TextSecondary, fontSize = 12.sp)
                }
                TextButton(onClick = onFavorite) { Text(if (item.favorite) "★ Saved" else "☆ Favorite") }
            }
            if (item.reason.isNotBlank()) Text(item.reason, color = TextSecondary, fontSize = 12.sp)
            Text(
                if (item.status.equals("PURCHASED", true)) {
                    "Actual: ${money(currency, item.actualPrice ?: item.estimatedPrice)} each · Subtotal: ${money(currency, (item.actualPrice ?: item.estimatedPrice) * item.quantity)}"
                } else "Estimate: ${money(item.currency, item.estimatedPrice)} each",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { onQuantityChange(item.quantity - 1) }, enabled = item.quantity > 1) { Text("−") }
                    Text("Qty ${item.quantity}", color = TextPrimary)
                    TextButton(onClick = { onQuantityChange(item.quantity + 1) }) { Text("+") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) { Text("Details / Edit") }
                    TextButton(onClick = onPurchase) { Text(if (item.status.equals("PURCHASED", true)) "Undo" else "Record purchase") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
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
