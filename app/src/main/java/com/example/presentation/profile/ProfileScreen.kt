package com.example.presentation.profile

import android.content.Context
import android.graphics.Bitmap
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.data.local.ApiKeyManager
import com.example.data.local.AppDatabase
import com.example.data.mapper.toDomain
import com.example.data.notification.OutfitReminderScheduler
import com.example.data.local.AppThemeMode
import com.example.domain.model.UserProfile
import com.example.domain.repository.AuthenticationRepository
import com.example.domain.usecase.UserProfileUseCase
import com.example.presentation.common.WearWiseButton
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseDrawables
import com.example.presentation.common.WearWiseTopBar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CoutureAmber
import com.example.ui.theme.CoutureEmerald
import com.example.ui.theme.CoutureRose
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DangerBorder
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ProfileViewModel(
    private val userProfileUseCase: UserProfileUseCase,
    private val apiKeyManager: ApiKeyManager,
    private val authenticationRepository: AuthenticationRepository,
    private val database: AppDatabase,
    private val appContext: Context
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    val customApiKey: StateFlow<String?> = apiKeyManager.customApiKeyFlow

    private val _isTestingKey = MutableStateFlow(false)
    val isTestingKey: StateFlow<Boolean> = _isTestingKey.asStateFlow()

    private val _testMessage = MutableStateFlow<String?>(null)
    val testMessage: StateFlow<String?> = _testMessage.asStateFlow()

    private val _isTestSuccess = MutableStateFlow<Boolean?>(null)
    val isTestSuccess: StateFlow<Boolean?> = _isTestSuccess.asStateFlow()

    private val _accountAction = MutableStateFlow(ProfileActionState())
    val accountAction: StateFlow<ProfileActionState> = _accountAction.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userProfileUseCase.getProfile().collect { p ->
                _profile.value = p
            }
        }
    }

    fun updateProfilePhoto(uriString: String) {
        viewModelScope.launch {
            _accountAction.value = ProfileActionState(isLoading = true)
            runCatching { authenticationRepository.updateProfilePhoto(uriString) }
                .onSuccess { url ->
                    val uid = authenticationRepository.state.value.user?.uid.orEmpty()
                    _profile.value = _profile.value?.copy(
                        profileImage = url,
                        profileImagePath = "users/$uid/profile/avatar.jpg"
                    )
                    _accountAction.value = ProfileActionState(message = "Profile photo updated.")
                }
                .onFailure { _accountAction.value = ProfileActionState(error = it.localizedMessage ?: "Photo upload failed.") }
        }
    }

    fun updateProfileBitmap(bitmap: Bitmap, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(context.cacheDir, "user_profile_avatar_${System.currentTimeMillis()}.jpg")
            val fos = file.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            updateProfilePhoto(android.net.Uri.fromFile(file).toString())
        }
    }

    fun saveCustomApiKey(key: String) {
        apiKeyManager.saveCustomApiKey(key)
        _testMessage.value = null
        _isTestSuccess.value = null
    }

    fun clearCustomApiKey() {
        apiKeyManager.clearCustomApiKey()
        _testMessage.value = null
        _isTestSuccess.value = null
    }

    fun testApiKey(key: String) {
        viewModelScope.launch {
            _isTestingKey.value = true
            _testMessage.value = "Pinging Google Gemini API..."
            _isTestSuccess.value = null

            val result = apiKeyManager.testApiKey(key)
            result.fold(
                onSuccess = { msg ->
                    _testMessage.value = "✓ $msg"
                    _isTestSuccess.value = true
                },
                onFailure = { err ->
                    _testMessage.value = "✕ Error: ${err.localizedMessage ?: "Invalid API Key"}"
                    _isTestSuccess.value = false
                }
            )
            _isTestingKey.value = false
        }
    }

    fun resetTestState() {
        _testMessage.value = null
        _isTestSuccess.value = null
    }

    fun resetPreferences() {
        viewModelScope.launch {
            userProfileUseCase.resetPreferences()
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            _accountAction.value = ProfileActionState(isLoading = true)
            runCatching { authenticationRepository.updateProfile(profile) }
                .onSuccess { _accountAction.value = ProfileActionState(message = "Profile updated.") }
                .onFailure { _accountAction.value = ProfileActionState(error = it.localizedMessage ?: "Profile update failed.") }
        }
    }

    fun changePassword(currentPassword: String, password: String, onSuccess: () -> Unit) {
        if (currentPassword.isBlank()) {
            _accountAction.value = ProfileActionState(error = "Enter your current password.")
            return
        }
        if (password.length < 8 || password.none(Char::isLetter) || password.none(Char::isDigit)) {
            _accountAction.value = ProfileActionState(error = "Use at least 8 characters with letters and numbers.")
            return
        }
        viewModelScope.launch {
            _accountAction.value = ProfileActionState(isLoading = true)
            runCatching { authenticationRepository.updatePassword(currentPassword, password) }
                .onSuccess { _accountAction.value = ProfileActionState(message = "Password changed."); onSuccess() }
                .onFailure { _accountAction.value = ProfileActionState(error = it.localizedMessage ?: "Password update failed.") }
        }
    }

    fun deleteAccount(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _accountAction.value = ProfileActionState(isLoading = true)
            runCatching { authenticationRepository.deleteAccount(password) }
                .onSuccess { _accountAction.value = ProfileActionState(); onSuccess() }
                .onFailure { _accountAction.value = ProfileActionState(error = it.localizedMessage ?: "Account deletion failed.") }
        }
    }

    fun clearAccountMessage() { _accountAction.value = ProfileActionState() }

    fun setOutfitNotificationsEnabled(enabled: Boolean) {
        appContext.getSharedPreferences(OutfitReminderScheduler.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(OutfitReminderScheduler.KEY_OUTFIT_REMINDERS, enabled).apply()
        viewModelScope.launch(Dispatchers.IO) {
            val uid = authenticationRepository.state.value.user?.uid ?: return@launch
            val scheduler = OutfitReminderScheduler(appContext)
            database.weeklyScheduleDao().getAllForOwner(uid).map { it.toDomain() }.forEach { plan ->
                if (enabled) scheduler.schedule(plan) else scheduler.cancel(plan.dateIso)
            }
        }
    }
}

data class ProfileActionState(val isLoading: Boolean = false, val error: String? = null, val message: String? = null)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    themeMode: AppThemeMode,
    emailVerified: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val profileState by viewModel.profile.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val isTestingKey by viewModel.isTestingKey.collectAsState()
    val testMessage by viewModel.testMessage.collectAsState()
    val isTestSuccess by viewModel.isTestSuccess.collectAsState()
    val accountAction by viewModel.accountAction.collectAsState()

    val profile = profileState ?: UserProfile()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var outfitNotificationsEnabled by remember(context) {
        mutableStateOf(OutfitReminderScheduler.notificationsEnabled(context))
    }

    // Avatar pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.updateProfilePhoto(uri.toString())
                Toast.makeText(context, "Try-On Avatar photo updated!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                viewModel.updateProfileBitmap(bitmap, context)
                Toast.makeText(context, "Try-On Avatar captured & saved!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .testTag("profile_screen")
    ) {
        WearWiseTopBar(
            title = "WEARWISE",
            subtitle = "CURATOR PROFILE",
            userAvatarRes = WearWiseDrawables.UserAvatar,
            showNotification = false
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Authenticated profile, observed from users/{uid} in Firestore.
            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, RoyalElectricViolet, CircleShape)
                            .background(CardElevated)
                    ) {
                        if (!profile.profileImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = profile.profileImage,
                                contentDescription = profile.firstName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Profile photo not set", tint = TextSecondary, modifier = Modifier.align(Alignment.Center).size(34.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.firstName.ifBlank { "WEARWISE MEMBER" }.uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            if (emailVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("VERIFIED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CoutureEmerald)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = profile.email, fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = profile.stylePreferences.joinToString().ifBlank { "Personal style profile" },
                            fontSize = 10.sp, color = CoutureAmber, maxLines = 1
                        )
                    }
                    TextButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = RoyalElectricViolet, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Edit", color = RoyalElectricViolet, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = RoyalElectricViolet)
            Spacer(modifier = Modifier.height(8.dp))
            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Appearance", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("Choose how WearWise looks on this device.", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onThemeModeChanged(mode) }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChanged(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = RoyalElectricViolet)
                            )
                            Column {
                                Text(mode.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                if (mode == AppThemeMode.SYSTEM) {
                                    Text("Follow your device setting", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Account Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, null, tint = CoutureAmber, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Outfit reminders", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Scheduled planner notifications", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                        Switch(checked = outfitNotificationsEnabled, onCheckedChange = { enabled ->
                            outfitNotificationsEnabled = enabled
                            viewModel.setOutfitNotificationsEnabled(enabled)
                        })
                    }
                    ProfileActionRow(Icons.Default.LockReset, "Change Password") { showPasswordDialog = true }
                    ProfileActionRow(Icons.Default.Notifications, "Notification Settings") {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }
                    ProfileActionRow(Icons.Default.Security, "Privacy & Security") {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}")))
                    }
                    ProfileActionRow(Icons.Default.Delete, "Delete Account", danger = true) { showDeleteAccountDialog = true }
                }
            }

            if (accountAction.error != null || accountAction.message != null) {
                Text(
                    accountAction.error ?: accountAction.message.orEmpty(),
                    color = if (accountAction.error != null) MaterialTheme.colorScheme.error else CoutureEmerald,
                    fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Personal Photo for Virtual Try-On
            Text("PERSONAL VIRTUAL TRY-ON AVATAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = CoutureAmber)
            Spacer(modifier = Modifier.height(8.dp))

            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Use your actual photo so AI applies outfits directly onto your body and silhouette.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera", fontSize = 12.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CoutureAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery", fontSize = 12.sp, color = CoutureAmber)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Gemini API Key Card
            Text("GEMINI AI ENGINE & API KEYS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = RoyalElectricViolet)
            Spacer(modifier = Modifier.height(8.dp))

            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = RoyalElectricViolet, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini API Key", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        val hasCustom = !customApiKey.isNullOrEmpty()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (hasCustom) Color(0x3310B981) else Color(0x336366F1))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (hasCustom) "CUSTOM KEY ACTIVE" else "STUDIO DEFAULT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasCustom) CoutureEmerald else RoyalElectricViolet
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (!customApiKey.isNullOrEmpty()) {
                            "Custom key active (ends in ...${customApiKey?.takeLast(4)}). Used for live high-res try-on generation."
                        } else {
                            "Connect your personal Gemini API key from Google AI Studio to unlock unlimited photorealistic virtual try-on renders."
                        },
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.resetTestState()
                                showApiKeyDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (customApiKey.isNullOrEmpty()) "Configure Key" else "Change Key",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (!customApiKey.isNullOrEmpty()) {
                            Button(
                                onClick = {
                                    viewModel.clearCustomApiKey()
                                    Toast.makeText(context, "Reverted to default AI key", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Text("Clear", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Neural Calibration Summary
            Text("CALIBRATION PROFILE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = CoutureAmber)
            Spacer(modifier = Modifier.height(8.dp))

            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalibrationRow("Dress Environment", profile.dressEnvironment)
                    CalibrationRow("Style DNA", profile.stylePreferences.joinToString(", "))
                    CalibrationRow("Silhouette Matrix", profile.silhouetteArchetype)
                    CalibrationRow("Microclimate", profile.climate)
                    CalibrationRow("Occasions", profile.commonOccasions.joinToString(", "))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset Preferences Button
            Button(
                onClick = {
                    viewModel.resetPreferences()
                    Toast.makeText(context, "Personal style preferences cleared.", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .testTag("reset_preferences_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = CoutureAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Learned Preferences", fontSize = 13.sp, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sign Out
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = DangerContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, DangerBorder, RoundedCornerShape(16.dp))
                    .testTag("sign_out_button")
            ) {
                Text("Sign Out / Switch Identity", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CoutureRose)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Configure API Key Dialog
    if (showApiKeyDialog) {
        var inputKey by remember { mutableStateOf(customApiKey ?: "") }
        var isPasswordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gemini API Key",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = { showApiKeyDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter your Google Gemini API key to run direct multimodal virtual try-on on your personal photos.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Test Connection generates a small real image to verify image access and uses your Gemini image quota.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("API Key (starts with AIzaSy...)") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalElectricViolet,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Test Connection Button & Result
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.testApiKey(inputKey) },
                            enabled = inputKey.isNotBlank() && !isTestingKey,
                            colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(color = RoyalElectricViolet, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 11.sp, color = TextPrimary)
                            } else {
                                Text("Test Connection", fontSize = 11.sp, color = CoutureAmber)
                            }
                        }

                        if (!testMessage.isNullOrEmpty()) {
                            Text(
                                text = testMessage ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTestSuccess == true) CoutureEmerald else Color(0xFFF43F5E),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputKey.isNotBlank()) {
                            viewModel.saveCustomApiKey(inputKey)
                            Toast.makeText(context, "Gemini API key saved securely!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.clearCustomApiKey()
                        }
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Key", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showApiKeyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkNavyBg,
            tonalElevation = 8.dp
        )
    }

    if (showEditProfileDialog) {
        var fullName by remember { mutableStateOf(profile.firstName) }
        var gender by remember { mutableStateOf(profile.gender) }
        var location by remember { mutableStateOf(profile.region) }
        var styles by remember { mutableStateOf(profile.stylePreferences.joinToString(", ")) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("Update only the personal details you choose to share.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    ProfileEditField("Full name", fullName) { fullName = it }
                    ProfileEditField("Gender (optional)", gender) { gender = it }
                    ProfileEditField("Location (optional)", location) { location = it }
                    ProfileEditField("Preferred styles (comma separated)", styles) { styles = it }
                }
            },
            confirmButton = {
                TextButton(enabled = !accountAction.isLoading, onClick = {
                    viewModel.saveProfile(profile.copy(
                        firstName = fullName.trim(), nickname = fullName.trim(), gender = gender.trim(),
                        region = location.trim(), stylePreferences = styles.split(',').map(String::trim).filter(String::isNotEmpty)
                    ))
                    showEditProfileDialog = false
                }) { Text(if (accountAction.isLoading) "Saving…" else "Save", color = RoyalElectricViolet) }
            },
            dismissButton = { TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel", color = TextSecondary) } },
            containerColor = DarkNavyBg
        )
    }

    if (showPasswordDialog) {
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; viewModel.clearAccountMessage() },
            title = { Text("Change Password", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    PasswordEditField("Current password", currentPassword) { currentPassword = it }
                    Spacer(Modifier.height(8.dp))
                    PasswordEditField("New password", newPassword) { newPassword = it }
                    Spacer(Modifier.height(8.dp))
                    PasswordEditField("Confirm password", confirmPassword) { confirmPassword = it }
                    if (accountAction.error != null) Text(accountAction.error.orEmpty(), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = !accountAction.isLoading, onClick = {
                    if (newPassword != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                    } else viewModel.changePassword(currentPassword, newPassword) { showPasswordDialog = false }
                }) { Text(if (accountAction.isLoading) "Updating…" else "Update", color = RoyalElectricViolet) }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false; viewModel.clearAccountMessage() }) { Text("Cancel", color = TextSecondary) } },
            containerColor = DarkNavyBg
        )
    }

    if (showDeleteAccountDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false; viewModel.clearAccountMessage() },
            title = { Text("Delete Account?", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("This permanently deletes your WearWise account and its cloud data. Enter your password to confirm.", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    PasswordEditField("Current password", password) { password = it }
                    if (accountAction.error != null) Text(accountAction.error.orEmpty(), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = password.isNotBlank() && !accountAction.isLoading, onClick = {
                    viewModel.deleteAccount(password) {
                        showDeleteAccountDialog = false
                        onSignOut()
                    }
                }) { Text(if (accountAction.isLoading) "Deleting…" else "Delete Account", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAccountDialog = false; viewModel.clearAccountMessage() }) { Text("Cancel", color = TextSecondary) } },
            containerColor = DarkNavyBg
        )
    }
}

@Composable
private fun ProfileActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) MaterialTheme.colorScheme.error else CoutureAmber, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, color = if (danger) MaterialTheme.colorScheme.error else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileEditField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = RoyalElectricViolet, unfocusedBorderColor = CardBorder),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun PasswordEditField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = RoyalElectricViolet, unfocusedBorderColor = CardBorder),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CalibrationRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1)
    }
}
