package com.example.presentation.coloranalysis

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.ColorSwatch
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseTopBar
import com.example.presentation.common.WearWiseDrawables
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun ColorAnalysisScreen(
    viewModel: ColorAnalysisViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newColorName by remember { mutableStateOf("") }
    var newColorHex by remember { mutableStateOf("#") }
    var isNewColorRecommended by remember { mutableStateOf(true) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val file = File(context.cacheDir, "color_analysis_${UUID.randomUUID()}.jpg")
                    FileOutputStream(file).use { output ->
                        inputStream?.copyTo(output)
                    }
                    viewModel.setPhotoPath(file.absolutePath)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                viewModel.setPhotoFromBitmap(bitmap, context)
            }
        }
    )

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.dismissError()
        }
        state.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Color Swatch", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Customize your palette by pinning favorite colors or adding personal exceptions.", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = newColorName,
                        onValueChange = { newColorName = it },
                        label = { Text("Color Name (e.g. Cobalt)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newColorHex,
                        onValueChange = { newColorHex = it },
                        label = { Text("HEX Code (e.g. #0047AB)") },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category:", color = TextPrimary, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isNewColorRecommended,
                                onClick = { isNewColorRecommended = true },
                                label = { Text("Recommended") }
                            )
                            FilterChip(
                                selected = !isNewColorRecommended,
                                onClick = { isNewColorRecommended = false },
                                label = { Text("To Avoid") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newColorName.isNotBlank() && newColorHex.isNotBlank()) {
                            viewModel.addCustomColor(newColorName, newColorHex, isNewColorRecommended)
                            newColorName = ""
                            newColorHex = "#"
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "Please enter both color name and HEX code", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet)
                ) {
                    Text("Add Swatch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkNavyBg
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkNavyBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "AI PERSONAL COLOR ANALYSIS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Disclaimer Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = CoutureAmber.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoutureAmber.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CoutureAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Aesthetic Styling Notice",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Lighting, camera filters, and screen calibration can affect analysis results. This tool provides aesthetic style guidance rather than a scientific or definitive classification.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Photo Capture / Selection Card
            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Face Photo for Undertone Analysis",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(RoyalElectricViolet.copy(alpha = 0.2f))
                            .border(2.dp, RoyalElectricViolet, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!state.photoPath.isNullOrBlank()) {
                            AsyncImage(
                                model = state.photoPath,
                                contentDescription = "Face Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No Photo",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Take Selfie")
                        }

                        Button(
                            onClick = { galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet.copy(alpha = 0.8f))
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Gallery")
                        }
                    }

                    Button(
                        onClick = { viewModel.analyzePhoto() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading && !state.photoPath.isNullOrBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CoutureAmber)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Analyzing with Gemini AI...", color = Color.Black)
                        } else {
                            Text("✨ Run Gemini Color Analysis", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Results Section
            if (state.colorSeason.isNotBlank()) {
                WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SEASONAL PALETTE",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.colorSeason.uppercase(),
                                    color = CoutureAmber,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Surface(
                                color = RoyalElectricViolet.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalElectricViolet)
                            ) {
                                Text(
                                    text = "Undertone: ${state.skinUndertone}",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CharacteristicsItem(label = "Hair", value = state.hairColorAnalyzed)
                            CharacteristicsItem(label = "Eyes", value = state.eyeColorAnalyzed)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recommended Colors",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            TextButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CoutureAmber, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Add Custom", color = CoutureAmber, fontSize = 12.sp)
                            }
                        }

                        ColorSwatchGrid(
                            swatches = state.recommendedColors,
                            onRemove = { swatch -> viewModel.removeColor(swatch, true) }
                        )

                        Text(
                            text = "Colors to Avoid",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        ColorSwatchGrid(
                            swatches = state.colorsToAvoid,
                            onRemove = { swatch -> viewModel.removeColor(swatch, false) }
                        )

                        // Rationale
                        Text(
                            text = "AI Stylist Analysis",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = state.rationale,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { viewModel.saveAnalysisToProfile() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isSaved) Color(0xFF2E7D32) else RoyalElectricViolet
                            )
                        ) {
                            if (state.isSaved) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Saved to Profile", color = Color.White)
                            } else {
                                Text("Save Analysis to Profile", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacteristicsItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        Text(text = value.ifBlank { "N/A" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ColorSwatchGrid(swatches: List<ColorSwatch>, onRemove: (ColorSwatch) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        swatches.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { swatch ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        color = parseHexColor(swatch.hex),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(parseHexColor(swatch.hex))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = swatch.name,
                                color = getContrastColor(swatch.hex),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.align(Alignment.BottomStart)
                            )
                            IconButton(
                                onClick = { onRemove(swatch) },
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = getContrastColor(swatch.hex).copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                // Fill remaining empty space in row if odd count
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex.trim()))
    } catch (_: Exception) {
        Color(0xFF4A3B5C)
    }
}

private fun getContrastColor(hex: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex.trim())
        val red = android.graphics.Color.red(colorInt)
        val green = android.graphics.Color.green(colorInt)
        val blue = android.graphics.Color.blue(colorInt)
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        if (luminance > 0.5) Color.Black else Color.White
    } catch (_: Exception) {
        Color.White
    }
}
