package com.example.presentation.onboarding

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.common.WearWiseButton
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseColorChip
import com.example.presentation.common.WearWiseDrawables
import com.example.presentation.common.WearWiseProfileTile
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
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 7

    // State bindings
    var firstName by remember { mutableStateOf("Yared") }
    var nickname by remember { mutableStateOf("Dr. Yared") }
    var selectedAgeRange by remember { mutableStateOf("25-34") }
    var selectedGender by remember { mutableStateOf("Male") }

    val selectedRoles = remember { mutableStateListOf("Lecturer / Academic", "Researcher") }
    var formalityLevel by remember { mutableFloatStateOf(6f) }
    val selectedEnvironments = remember { mutableStateListOf("Smart Casual", "Business Casual") }

    val selectedStyles = remember { mutableStateListOf("Smart Casual", "Minimalist", "Classic", "Modern Tech") }

    val favoriteColors = remember {
        mutableStateListOf(
            "Deep Navy", "Charcoal Grey", "Crisp White", "Olive Green", "Camel / Tan", "Black / Noir"
        )
    }
    val avoidedColors = remember { mutableStateListOf("Neon Yellow", "Bright Hot Pink") }

    val selectedOccasions = remember {
        mutableStateListOf(
            "Lectures & Teaching", "Research Lab & Colloquium", "Faculty & Board Meetings", "Weekend Outings", "Dining & Date Night"
        )
    }

    var selectedSilhouette by remember { mutableStateOf("Athletic Lean") }
    var selectedStature by remember { mutableStateOf("6'0\"-6'3\"") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .testTag("onboarding_screen")
    ) {
        // Sticky Header with Progress Bar and Skip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentStep > 1) currentStep--
                    },
                    enabled = currentStep > 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (currentStep > 1) TextPrimary else TextMuted
                    )
                }

                Text(
                    text = "WEARWISE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = TextPrimary
                )

                Text(
                    text = "SKIP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalElectricViolet,
                    modifier = Modifier
                        .clickable(onClick = onComplete)
                        .padding(8.dp)
                        .testTag("onboarding_skip_button")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFF1E2435))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = currentStep.toFloat() / totalSteps)
                        .height(3.dp)
                        .background(RoyalElectricViolet)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STEP $currentStep OF $totalSteps",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalElectricViolet
                )
                Text(
                    text = when (currentStep) {
                        1 -> "Personal Profile"
                        2 -> "Lifestyle & Routine"
                        3 -> "Dress Environment"
                        4 -> "Signature Aesthetic"
                        5 -> "Chromatic Harmony"
                        6 -> "Occasion Cadence"
                        else -> "Virtual Twin Calibration"
                    },
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        // Body Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentStep) {
                1 -> Step1PersonalProfile(
                    firstName = firstName,
                    onFirstNameChange = { firstName = it },
                    nickname = nickname,
                    onNicknameChange = { nickname = it },
                    selectedAge = selectedAgeRange,
                    onAgeSelect = { selectedAgeRange = it },
                    selectedGender = selectedGender,
                    onGenderSelect = { selectedGender = it }
                )
                2 -> Step2Lifestyle(
                    selectedRoles = selectedRoles,
                    onToggleRole = { role ->
                        if (selectedRoles.contains(role)) selectedRoles.remove(role) else selectedRoles.add(role)
                    }
                )
                3 -> Step3DressEnvironment(
                    level = formalityLevel,
                    onLevelChange = { formalityLevel = it },
                    selectedEnvironments = selectedEnvironments,
                    onToggleEnv = { env ->
                        if (selectedEnvironments.contains(env)) selectedEnvironments.remove(env) else selectedEnvironments.add(env)
                    }
                )
                4 -> Step4SignatureStyle(
                    selectedStyles = selectedStyles,
                    onToggleStyle = { style ->
                        if (selectedStyles.contains(style)) selectedStyles.remove(style) else selectedStyles.add(style)
                    }
                )
                5 -> Step5ColorPalette(
                    favoriteColors = favoriteColors,
                    avoidedColors = avoidedColors,
                    onToggleFavorite = { c ->
                        if (favoriteColors.contains(c)) favoriteColors.remove(c) else favoriteColors.add(c)
                    },
                    onToggleAvoided = { c ->
                        if (avoidedColors.contains(c)) avoidedColors.remove(c) else avoidedColors.add(c)
                    }
                )
                6 -> Step6Occasions(
                    selectedOccasions = selectedOccasions,
                    onToggleOccasion = { o ->
                        if (selectedOccasions.contains(o)) selectedOccasions.remove(o) else selectedOccasions.add(o)
                    }
                )
                7 -> Step7VirtualTwin(
                    selectedSilhouette = selectedSilhouette,
                    onSelectSilhouette = { selectedSilhouette = it },
                    selectedStature = selectedStature,
                    onSelectStature = { selectedStature = it }
                )
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkNavyBg)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 1) {
                Button(
                    onClick = { currentStep-- },
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
                    Text("Back", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            WearWiseButton(
                text = if (currentStep == totalSteps) "COMPLETE & ENTER WEARWISE" else "CONTINUE",
                onClick = {
                    if (currentStep < totalSteps) {
                        currentStep++
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier.weight(if (currentStep > 1) 2f else 1f),
                trailingIcon = if (currentStep == totalSteps) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    }
}

@Composable
private fun Step1PersonalProfile(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    selectedAge: String,
    onAgeSelect: (String) -> Unit,
    selectedGender: String,
    onGenderSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161A2B))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "• COUTURE AI MATRIX",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalElectricViolet
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "What should we know about you?",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Help your neural stylist calibrate proportions, climate physics, and cultural aesthetic nuances.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        WearWiseProfileTile(
            name = "$firstName • Cambridge Synced",
            subtitle = "Calibrating silhouette and climate engine"
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "FIRST NAME / PREFERRED NAME",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardElevated,
                unfocusedContainerColor = CardElevated,
                focusedBorderColor = RoyalElectricViolet,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "NICKNAME (OPTIONAL)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardElevated,
                unfocusedContainerColor = CardElevated,
                focusedBorderColor = RoyalElectricViolet,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "AGE RANGE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("18-24", "25-34", "35-44", "45-54", "55+").forEach { range ->
                val selected = selectedAge == range
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) RoyalElectricViolet else CardElevated)
                        .border(1.dp, if (selected) RoyalElectricViolet else CardBorder, RoundedCornerShape(20.dp))
                        .clickable { onAgeSelect(range) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "GENDER IDENTITY & PRESENTATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Male", "Female", "Non-binary").forEach { gender ->
                val selected = selectedGender == gender
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) RoyalElectricViolet else CardElevated)
                        .border(1.dp, if (selected) RoyalElectricViolet else CardBorder, RoundedCornerShape(20.dp))
                        .clickable { onGenderSelect(gender) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gender,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Location & Microclimate Card (Image 1)
        WearWiseCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RoyalElectricViolet, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Location & Microclimate", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text("AUTO SYNC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CoutureEmerald)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("United States • Cambridge, MA", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = CoutureAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hyper-local weather sync enabled • 58°F (14°C)", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun Step2Lifestyle(
    selectedRoles: List<String>,
    onToggleRole: (String) -> Unit
) {
    val roles = listOf(
        "Lecturer / Academic" to "SMART CASUAL",
        "Researcher" to "LAB & COLLOQUIUM",
        "Student" to "CAMPUS COMFORT",
        "Tech & Developer" to "TECH EASE",
        "Doctor / Health" to "SCRUBS SYNC",
        "Entrepreneur" to "PITCH READY",
        "Business Pro" to "BOARDROOM",
        "Creative & Art" to "AVANT-GARDE",
        "Freelancer" to "HOME / NOMAD"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161A2B))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("CONTEXT ENGINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalElectricViolet)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "What best describes your daily life?",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select one or more that describe your routine. Your AI stylist adapts outfit recommendations to your profession's dress codes.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        roles.chunked(2).forEach { rowRoles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowRoles.forEach { (role, badge) ->
                    val selected = selectedRoles.contains(role)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) RoyalElectricViolet else CardElevated)
                            .border(1.dp, if (selected) RoyalElectricViolet else CardBorder, RoundedCornerShape(16.dp))
                            .clickable { onToggleRole(role) }
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = if (role.contains("Academic") || role.contains("Student")) Icons.Default.School else Icons.Default.Science,
                                    contentDescription = null,
                                    tint = if (selected) Color.White else RoyalElectricViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = role,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selected) Color(0x33000000) else Color(0x336366F1))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) CoutureAmber else RoyalElectricViolet
                                )
                            }
                        }
                    }
                }
                if (rowRoles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected roles indicator
        WearWiseCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CoutureAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("${selectedRoles.size} roles selected", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Academic & Research blend detected", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun Step3DressEnvironment(
    level: Float,
    onLevelChange: (Float) -> Unit,
    selectedEnvironments: List<String>,
    onToggleEnv: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161A2B))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("CONTEXT INTELLIGENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalElectricViolet)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Where do you usually dress for?",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select typical dress environments you navigate throughout the week.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Formality Slider
        WearWiseCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CALCULATED SPAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text("Smart Casual to Business (Lvl ${level.toInt()})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoutureAmber)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = level,
                    onValueChange = onLevelChange,
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = RoyalElectricViolet,
                        activeTrackColor = RoyalElectricViolet,
                        inactiveTrackColor = CardBorder
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Relaxed (1)", fontSize = 10.sp, color = TextMuted)
                    Text("Everyday (5)", fontSize = 10.sp, color = TextMuted)
                    Text("Executive (10)", fontSize = 10.sp, color = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        listOf(
            Triple("Smart Casual", "Lvl 5/10", "Elevated Relaxed • Chinos, knitwear, unlined blazers"),
            Triple("Business Casual", "Lvl 7/10", "Corporate Modern • Trousers, collared shirts, loafers"),
            Triple("Casual", "Lvl 2/10", "Everyday Ease • Coffee, lounging, relaxed denim"),
            Triple("Formal / Executive", "Lvl 9/10", "Boardrooms, gala, dark suits, structured pieces")
        ).forEach { (title, lvl, desc) ->
            val selected = selectedEnvironments.contains(title)
            WearWiseCard(
                isSelected = selected,
                onClick = { onToggleEnv(title) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF262A3D))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(lvl, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CoutureAmber)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(desc, fontSize = 12.sp, color = TextSecondary)
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(RoyalElectricViolet),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step4SignatureStyle(
    selectedStyles: List<String>,
    onToggleStyle: (String) -> Unit
) {
    val archetypes = listOf(
        Pair("Smart Casual", "Knitwear, tailored trousers, unlined blazers."),
        Pair("Minimalist", "Monochromatic cuts, architectural forms, zero excess."),
        Pair("Classic", "Tailored oxford shirts, double-face wool, heritage."),
        Pair("Modern Tech", "Sleek weather-sealed layers, urban technical textiles."),
        Pair("Executive", "Peak lapel tailoring, silk accents, polished calfskin."),
        Pair("Avant-Garde", "Asymmetrical lines, sculptural drapes, statement textures.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161A2B))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("STYLE MATRIX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalElectricViolet)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "What's your signature style?",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose your favorite visual aesthetics. WearWise balances these archetypes across daily ensemble recommendations.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        archetypes.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { (title, desc) ->
                    val selected = selectedStyles.contains(title)
                    WearWiseCard(
                        isSelected = selected,
                        onClick = { onToggleStyle(title) },
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) RoyalElectricViolet else TextPrimary
                                )
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = RoyalElectricViolet, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        WearWiseCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B2032)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("94%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoutureEmerald)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Style Cohesion Resonance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Harmonious daily rotation forecast: OPTIMAL", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun Step5ColorPalette(
    favoriteColors: List<String>,
    avoidedColors: List<String>,
    onToggleFavorite: (String) -> Unit,
    onToggleAvoided: (String) -> Unit
) {
    val colorMap = listOf(
        Pair("Deep Navy", Color(0xFF1E293B)),
        Pair("Charcoal Grey", Color(0xFF334155)),
        Pair("Crisp White", Color(0xFFF8FAFC)),
        Pair("Olive Green", Color(0xFF4D6043)),
        Pair("Camel / Tan", Color(0xFFC29B62)),
        Pair("Black / Noir", Color(0xFF0F172A)),
        Pair("Burgundy / Wine", Color(0xFF6B1D2F)),
        Pair("Cobalt Blue", Color(0xFF2563EB)),
        Pair("Slate Blue", Color(0xFF475569)),
        Pair("Terracotta / Rust", Color(0xFFC25E3B)),
        Pair("Neon Yellow", Color(0xFFEAB308)),
        Pair("Bright Hot Pink", Color(0xFFEC4899))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161A2B))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("COLOR PALETTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalElectricViolet)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Define your color palette",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Train the AI color coordination algorithm on the tones you love wearing and the shades to avoid.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text("FAVORITE COLORS (${favoriteColors.size} SELECTED)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoutureAmber)
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            colorMap.take(8).chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (name, col) ->
                        WearWiseColorChip(
                            colorName = name,
                            hexColor = col,
                            isSelected = favoriteColors.contains(name),
                            onToggle = { onToggleFavorite(name) },
                            isExcluded = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("AVOIDED COLORS (${avoidedColors.size} EXCLUDED)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colorMap.takeLast(2).forEach { (name, col) ->
                WearWiseColorChip(
                    colorName = name,
                    hexColor = col,
                    isSelected = avoidedColors.contains(name),
                    onToggle = { onToggleAvoided(name) },
                    isExcluded = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Calibrated Spectrum Card (Image 7)
        WearWiseCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calibrated Spectrum", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("COHESION: 96%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoutureAmber)
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Spectrum color bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Box(modifier = Modifier.weight(2f).background(Color(0xFF1E293B)))
                    Box(modifier = Modifier.weight(2f).background(Color(0xFF4D6043)))
                    Box(modifier = Modifier.weight(2f).background(Color(0xFFC29B62)))
                    Box(modifier = Modifier.weight(2f).background(Color(0xFF334155)))
                    Box(modifier = Modifier.weight(2f).background(Color(0xFFF8FAFC)))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Base Anchor: Deep Navy & Charcoal | Accent: Warm Camel & Olive", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun Step6Occasions(
    selectedOccasions: List<String>,
    onToggleOccasion: (String) -> Unit
) {
    val occasions = listOf(
        "Lectures & Teaching" to "3x / week",
        "Research Lab & Colloquium" to "4x / week",
        "Faculty & Board Meetings" to "2x / week",
        "Weekend Outings" to "Frequent",
        "Dining & Date Night" to "2x / week",
        "Academic Conferences" to "Monthly",
        "Gym & Active Recovery" to "3x / week"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161A2B))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("LIFESTYLE SCHEDULER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalElectricViolet)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "What do you dress for most often?",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select frequent occasions in your life. WearWise pre-seeds recommendations for your weekly schedule.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        occasions.forEach { (name, freq) ->
            val selected = selectedOccasions.contains(name)
            WearWiseCard(
                isSelected = selected,
                onClick = { onToggleOccasion(name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(freq, fontSize = 11.sp, color = CoutureAmber)
                    }
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(RoyalElectricViolet),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step7VirtualTwin(
    selectedSilhouette: String,
    onSelectSilhouette: (String) -> Unit,
    selectedStature: String,
    onSelectStature: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161A2B))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("3D CALIBRATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalElectricViolet)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Calibrate Your Virtual Try-On",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Calibrate your proportions for photorealistic cloth physics and hyper-accurate draping.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Full body model preview card (Image 22)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Image(
                painter = painterResource(id = WearWiseDrawables.TryOnModel),
                contentDescription = "Calibration Twin",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC0B0D14))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "• 60 FPS • NEURAL MESH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoutureEmerald
                )
            }

            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC0B0D14))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Text(
                    text = "FIDELITY: 98.4% MATCH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoutureAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text("SILHOUETTE ARCHETYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Athletic Lean", "Slim Tailored").forEach { arch ->
                val selected = selectedSilhouette == arch
                WearWiseCard(
                    isSelected = selected,
                    onClick = { onSelectSilhouette(arch) },
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Text(arch, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) RoyalElectricViolet else TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (arch == "Athletic Lean") "Tapered V-torso, 40\" chest" else "Streamlined, narrow taper", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("VERTICAL STATURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Petite", "5'8\"-5'11\"", "6'0\"-6'3\"", "6'4\"+").forEach { stat ->
                val selected = selectedStature == stat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) RoyalElectricViolet else CardElevated)
                        .border(1.dp, if (selected) RoyalElectricViolet else CardBorder, RoundedCornerShape(16.dp))
                        .clickable { onSelectStature(stat) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
