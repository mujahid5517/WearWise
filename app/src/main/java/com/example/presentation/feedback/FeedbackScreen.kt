package com.example.presentation.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.common.WearWiseTopBar
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    onNavigateToProfile: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
    ) {
        WearWiseTopBar(
            title = "WEARWISE",
            subtitle = "STYLE FEEDBACK",
            onAvatarClick = onNavigateToProfile
        )
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Outfit Feedback History",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Track your ratings and learned style preferences",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
