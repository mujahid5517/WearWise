package com.example.presentation.authentication

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.common.WearWiseButton
import com.example.presentation.common.WearWiseDrawables
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CoutureAmber
import com.example.ui.theme.CoutureEmerald
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .testTag("welcome_screen")
    ) {
        // Runway Hero Background Image (Image 19)
        Image(
            painter = painterResource(id = WearWiseDrawables.HeroRunway),
            contentDescription = "Runway Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.2f),
                            DarkNavyBg.copy(alpha = 0.95f),
                            DarkNavyBg
                        ),
                        startY = 0f,
                        endY = 1600f
                    )
                )
        )

        // Top Telemetry Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0B0D14))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "• WEARWISE AI COUTURE • V2.4",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0B0D14))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CoutureAmber,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Neural Core",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoutureAmber
                    )
                }
            }
        }

        // Bottom Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AUTOMATED HAUTE INTELLIGENCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = CoutureAmber
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Wear Smarter Every Day",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your AI-powered personal stylist, wardrobe organizer, and outfit planner tailored to your profession and lifestyle.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4 Mini Feature Pills Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeaturePill(
                    index = "01",
                    title = "Digital Wardrobe",
                    subtitle = "• Digitize in Secs",
                    icon = Icons.Default.Checkroom,
                    modifier = Modifier.weight(1f)
                )
                FeaturePill(
                    index = "02",
                    title = "AI Styling",
                    subtitle = "• Dynamic Harmony",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeaturePill(
                    index = "03",
                    title = "Smart Outfits",
                    subtitle = "• Context-Aware",
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
                FeaturePill(
                    index = "04",
                    title = "Virtual Preview",
                    subtitle = "• Photoreal Fit",
                    icon = Icons.Default.Person,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            WearWiseButton(
                text = "CREATE ACCOUNT",
                onClick = onNavigateToCreateAccount,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNavigateToSignIn,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardElevated,
                    contentColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
                    .testTag("signin_button")
            ) {
                Text(
                    text = "Sign In",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "By continuing, you agree to WearWise Terms of Service & Privacy Policy.",
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeaturePill(
    index: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardElevated.copy(alpha = 0.85f))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RoyalElectricViolet,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = index,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = CoutureAmber
            )
        }
    }
}

@Composable
fun CreateAccountScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onSignIn: () -> Unit
) {
    var firstName by remember { mutableStateOf("Yared") }
    var email by remember { mutableStateOf("yared@institution.edu") }
    var password by remember { mutableStateOf("••••••••••••") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("create_account_screen")
    ) {
        // Step Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STEP 01 / 04",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = RoyalElectricViolet
            )
            Text(
                text = "NEURAL PERSONA PROFILE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161A2B))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "• BESPOKE IDENTITY ENGINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = RoyalElectricViolet
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "CREATE YOUR WEARWISE ACCOUNT",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Unlock your AI-curated digital closet and personalized daily styling algorithms tailored to your cadence.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Curator Portrait Uploader Box (Image 26)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = WearWiseDrawables.UserAvatar),
                    contentDescription = "Curator Portrait",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(2.dp, RoyalElectricViolet, CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Curator Portrait",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33F59E0B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OPTIONAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoutureAmber
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Used to calibrate dynamic color & silhouette sync",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = RoyalElectricViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PHOTO ATTACHED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalElectricViolet
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // First Name Input
        Text(
            text = "FIRST NAME",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            placeholder = { Text("e.g. Yared", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = RoyalElectricViolet)
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardElevated,
                unfocusedContainerColor = CardElevated,
                focusedBorderColor = RoyalElectricViolet,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("first_name_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EMAIL ADDRESS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
            Text(
                text = "EDU COMPATIBLE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CoutureAmber
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("name@institution.edu or personal", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, tint = RoyalElectricViolet)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardElevated,
                unfocusedContainerColor = CardElevated,
                focusedBorderColor = RoyalElectricViolet,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SECURITY KEY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
            Text(
                text = "8+ CHARS, 1 SYMBOL",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = RoyalElectricViolet)
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Password",
                        tint = TextSecondary
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardElevated,
                unfocusedContainerColor = CardElevated,
                focusedBorderColor = RoyalElectricViolet,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Continue Button
        WearWiseButton(
            text = "CONTINUE TO STYLE CALIBRATION",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(CardBorder)
            )
            Text(
                text = "  OR CONTINUE WITH  ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextMuted
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(CardBorder)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SSO Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardElevated)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .clickable { onContinue() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = CoutureAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "University SSO",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Institutional discount & timetable outfit sync",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x3310B981))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AUTO SYNC",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoutureEmerald
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already in the circle? ",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text(
                text = "Sign In",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalElectricViolet,
                modifier = Modifier.clickable(onClick = onSignIn)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🛡️ 256-Bit Cryptographic Vault & Zero Public Crawling",
            fontSize = 11.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
