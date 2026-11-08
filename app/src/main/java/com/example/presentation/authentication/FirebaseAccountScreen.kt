package com.example.presentation.authentication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.common.WearWiseDrawables

@Composable
fun FirebaseAccountScreen(
    viewModel: AuthViewModel,
    signIn: Boolean,
    onSuccess: () -> Unit,
    onToggleMode: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val authenticationState by viewModel.authenticationState.collectAsState()
    val profileSyncFailed = authenticationState.user != null &&
        authenticationState.profileSyncStatus == com.example.domain.repository.ProfileSyncStatus.FAILED
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }

    val scheme = MaterialTheme.colorScheme
    val background = scheme.background
    val surface = scheme.surface
    val textColor = scheme.onSurface
    val secondaryText = scheme.onSurfaceVariant
    val passwordStrength = listOf(
        password.length >= 8,
        password.any(Char::isLetter),
        password.any(Char::isDigit),
        password.any { !it.isLetterOrDigit() }
    ).count { it }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .testTag(if (signIn) "signin_screen" else "create_account_screen")
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(scheme.primary.copy(alpha = 0.18f), background.copy(alpha = 0f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("auth_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "WEARWISE",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.2.sp
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(WearWiseDrawables.WearWiseIcon),
                    contentDescription = "WearWise logo",
                    modifier = Modifier.size(58.dp)
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = if (signIn) "Welcome back" else "Create Your WearWise Account",
                color = textColor,
                fontSize = if (signIn) 28.sp else 25.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (signIn) {
                    "Sign in to continue your personal style journey."
                } else {
                    "Your personal AI fashion stylist starts here."
                },
                color = secondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(surface)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!signIn) {
                    AuthTextField(
                        value = fullName,
                        onValueChange = { fullName = it; viewModel.clearFeedback() },
                        label = "Full name",
                        leadingIcon = Icons.Default.Person,
                        keyboardType = KeyboardType.Text,
                        testTag = "auth_full_name"
                    )
                }

                AuthTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearFeedback() },
                    label = "Email address",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    testTag = "auth_email"
                )

                AuthPasswordField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearFeedback() },
                    label = "Password",
                    visible = passwordVisible,
                    onToggleVisibility = { passwordVisible = !passwordVisible },
                    imeAction = if (signIn) ImeAction.Done else ImeAction.Next,
                    testTag = "auth_password"
                )

                if (signIn) {
                    Text(
                        text = "Forgot password?",
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable(enabled = !uiState.isLoading) {
                                viewModel.sendPasswordReset(email)
                            }
                            .testTag("forgot_password"),
                        color = scheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    PasswordStrengthIndicator(passwordStrength)
                    AuthPasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; viewModel.clearFeedback() },
                        label = "Confirm password",
                        visible = passwordVisible,
                        onToggleVisibility = { passwordVisible = !passwordVisible },
                        imeAction = ImeAction.Done,
                        testTag = "auth_confirm_password"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !uiState.isLoading) { termsAccepted = !termsAccepted }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            enabled = !uiState.isLoading
                        )
                        Text(
                            text = "I agree to the Terms of Service and Privacy Policy.",
                            color = secondaryText,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                AnimatedVisibility(
                    visible = uiState.error != null || uiState.message != null || profileSyncFailed,
                    enter = fadeIn() + slideInVertically { it / 3 },
                    exit = fadeOut()
                ) {
                    Text(
                        text = uiState.error
                            ?: authenticationState.profileSyncError
                            ?: uiState.message.orEmpty(),
                        color = if (uiState.error != null || profileSyncFailed) scheme.error else scheme.tertiary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("auth_feedback")
                    )
                }

                Button(
                    onClick = {
                        if (profileSyncFailed) {
                            viewModel.retryProfileSync(onSuccess)
                        } else if (signIn) {
                            viewModel.signIn(email, password, onSuccess)
                        } else {
                            viewModel.register(
                                fullName,
                                email,
                                password,
                                confirmPassword,
                                termsAccepted,
                                onSuccess
                            )
                        }
                    },
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("auth_submit")
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = scheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            if (profileSyncFailed) "Syncing profile…"
                            else if (signIn) "Signing in…"
                            else "Creating account…",
                            modifier = Modifier.padding(start = 10.dp),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            if (profileSyncFailed) "Retry Profile Sync"
                            else if (signIn) "Sign In"
                            else "Create Account",
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (signIn) "Don't have an account? " else "Already have an account? ",
                    color = secondaryText,
                    fontSize = 13.sp
                )
                Text(
                    if (signIn) "Create Account" else "Sign In",
                    color = scheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = !uiState.isLoading, onClick = onToggleMode)
                        .testTag("auth_toggle_mode")
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    )
}

@Composable
private fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    imeAction: ImeAction,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    )
}

@Composable
private fun PasswordStrengthIndicator(strength: Int) {
    val scheme = MaterialTheme.colorScheme
    val strengthColor by animateColorAsState(
        targetValue = when (strength) {
            0, 1 -> scheme.error
            2, 3 -> scheme.secondary
            else -> scheme.tertiary
        },
        label = "password-strength-color"
    )
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (strength > index) strengthColor else scheme.outlineVariant)
                )
            }
        }
        Text(
            text = when (strength) {
                0 -> "Use 8+ characters, letters, and numbers"
                1 -> "Weak"
                2 -> "Fair"
                3 -> "Good"
                else -> "Strong"
            },
            color = if (strength == 0) scheme.onSurfaceVariant else strengthColor,
            fontSize = 11.sp
        )
    }
}
