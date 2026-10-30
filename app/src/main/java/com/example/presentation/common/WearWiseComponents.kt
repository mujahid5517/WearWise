package com.example.presentation.common

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.local.AppLanguage
import com.example.data.local.LocalLanguageManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CardSelected
import com.example.ui.theme.ChipActiveBg
import com.example.ui.theme.ChipActiveBorder
import com.example.ui.theme.ChipInactiveBg
import com.example.ui.theme.ChipInactiveBorder
import com.example.ui.theme.CoutureAmber
import com.example.ui.theme.CoutureEmerald
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WearWiseTopBar(
    title: String? = null,
    subtitle: String? = null,
    showNotification: Boolean = true,
    userAvatarRes: Int? = WearWiseDrawables.UserAvatar,
    onAvatarClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val languageManager = LocalLanguageManager.current
    val currentLang = languageManager?.currentLanguage?.collectAsState()?.value ?: AppLanguage.ENGLISH
    val displayTitle = title ?: languageManager?.getString("app_title", currentLang) ?: "WEARWISE"
    val displaySubtitle = subtitle ?: languageManager?.getString("app_subtitle", currentLang) ?: "AI COUTURE"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkNavyBg)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("wearwise_top_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = displayTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                color = TextPrimary
            )
            if (displaySubtitle.isNotEmpty()) {
                Text(
                    text = displaySubtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = CoutureAmber
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (languageManager != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated)
                        .border(1.dp, RoyalElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { languageManager.toggleLanguage() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("language_toggle_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentLang.nativeName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoutureAmber
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (showNotification) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardElevated)
                        .clickable(onClick = onNotificationClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            if (userAvatarRes != null) {
                Image(
                    painter = painterResource(id = userAvatarRes),
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, RoyalElectricViolet, CircleShape)
                        .clickable(onClick = onAvatarClick)
                )
            }
        }
    }
}

@Composable
fun WearWiseBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val languageManager = LocalLanguageManager.current
    val currentLang = languageManager?.currentLanguage?.collectAsState()?.value ?: AppLanguage.ENGLISH

    val items = listOf(
        NavigationItem("outfits", languageManager?.getString("nav_outfits", currentLang) ?: "Outfits", Icons.Default.AutoAwesome),
        NavigationItem("closet", languageManager?.getString("nav_closet", currentLang) ?: "Closet", Icons.Default.Checkroom),
        NavigationItem("avatar", languageManager?.getString("nav_avatar", currentLang) ?: "Avatar", Icons.Default.Person),
        NavigationItem("planner", languageManager?.getString("nav_planner", currentLang) ?: "Planner", Icons.Default.CalendarMonth),
        NavigationItem("profile", languageManager?.getString("nav_profile", currentLang) ?: "Profile", Icons.Default.Tune)
    )

    Surface(
        color = CardElevated,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wearwise_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val contentColor by animateColorAsState(
                    targetValue = if (selected) RoyalElectricViolet else TextMuted,
                    label = "BottomBarColor"
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("nav_tab_${item.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

private data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun WearWiseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    backgroundColor: Color = RoyalElectricViolet
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        modifier = modifier
            .height(56.dp)
            .testTag("wearwise_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun WearWiseCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val borderColor = if (isSelected) RoyalElectricViolet else CardBorder
    val backgroundColor = if (isSelected) CardSelected else CardElevated

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun WearWiseProfileTile(
    name: String = "Dr. Yared",
    subtitle: String = "Cambridge Synced • Calibrating silhouette",
    avatarRes: Int = WearWiseDrawables.UserAvatar,
    modifier: Modifier = Modifier
) {
    WearWiseCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(2.dp, RoyalElectricViolet, CircleShape)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "ACTIVE IDENTITY BASE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = CoutureAmber
                )
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun WearWiseClothingCard(
    item: ClothingItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardElevated)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("clothing_card_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color(0xFF161A28))
            ) {
                if (item.imageRes != null) {
                    Image(
                        painter = painterResource(id = item.imageRes),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!item.imageUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.imageUri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Category pill
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC0B0D14))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = item.category.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }

                // 3 dots icon
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ChipActiveBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.style,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalElectricViolet
                        )
                    }

                    Text(
                        text = "*${item.formalityScore}/10",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoutureAmber
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.season} • ${item.fabric}",
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun WearWiseOutfitCard(
    outfit: Outfit,
    onWearThis: () -> Unit = {},
    onTryAnother: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wearwise_outfit_card")
    ) {
        // Hero Outfit Blueprint Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
        ) {
            if (outfit.previewImageRes != null) {
                Image(
                    painter = painterResource(id = outfit.previewImageRes),
                    contentDescription = outfit.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dark vignette gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Top tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LOOK #01 • ${outfit.occasion.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CoutureAmber)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Curated for Faculty",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Customize",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom title on image
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = "ENSEMBLE BLUEPRINT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = TextSecondary
                )
                Text(
                    text = outfit.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CoutureEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WEATHER READY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoutureEmerald
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Stylist Rationale Card
        WearWiseCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = RoyalElectricViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Stylist Rationale",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "OPTIMAL MATCH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = CoutureAmber
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = outfit.rationale,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resonance Breakdown Telemetry Metrics
        WearWiseCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resonance Breakdown",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Neural Index ${outfit.score / 10.0}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoutureEmerald
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ResonanceGauge(value = "${outfit.occasionFitScore}%", label = "Occasion Fit")
                    ResonanceGauge(value = "${outfit.thermalSyncScore}%", label = "Thermal Sync")
                    ResonanceGauge(value = "${outfit.colorHarmonyScore}%", label = "Color Harmony")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WearWiseButton(
                text = "Wear This",
                onClick = onWearThis,
                modifier = Modifier.weight(1f),
                trailingIcon = Icons.Default.Check
            )
            Button(
                onClick = onTryAnother,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardElevated,
                    contentColor = TextPrimary
                ),
                modifier = Modifier
                    .height(56.dp)
                    .weight(1f)
                    .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
            ) {
                Text(
                    text = "Try Another",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ResonanceGauge(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF161B2C))
                .border(2.dp, RoyalElectricViolet, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
fun WearWiseColorChip(
    colorName: String,
    hexColor: Color,
    isSelected: Boolean,
    onToggle: () -> Unit,
    isExcluded: Boolean = false
) {
    val bg = if (isSelected) ChipActiveBg else ChipInactiveBg
    val border = if (isSelected) ChipActiveBorder else ChipInactiveBorder

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(24.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("color_chip_$colorName")
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(hexColor)
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = colorName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isExcluded) Icons.Default.Close else Icons.Default.Check,
                contentDescription = null,
                tint = if (isExcluded) Color(0xFFEF4444) else RoyalElectricViolet,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun WearWiseLoadingView(
    message: String = "Calibrating Neural Silhouette..."
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = RoyalElectricViolet,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun WearWiseEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CardElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Checkroom,
                contentDescription = null,
                tint = RoyalElectricViolet,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
