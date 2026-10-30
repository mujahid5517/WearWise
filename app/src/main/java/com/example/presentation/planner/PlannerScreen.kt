package com.example.presentation.planner

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.WeeklyScheduleItem
import com.example.domain.usecase.WeeklyPlannerUseCase
import com.example.presentation.common.WearWiseButton
import com.example.presentation.common.WearWiseCard
import com.example.presentation.common.WearWiseDrawables
import com.example.presentation.common.WearWiseTopBar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CoutureAmber
import com.example.ui.theme.CoutureEmerald
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.RoyalElectricViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlannerViewModel(
    private val weeklyPlannerUseCase: WeeklyPlannerUseCase
) : ViewModel() {

    private val _schedules = MutableStateFlow<List<WeeklyScheduleItem>>(emptyList())
    val schedules: StateFlow<List<WeeklyScheduleItem>> = _schedules.asStateFlow()

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            weeklyPlannerUseCase.getSchedule().collect { list ->
                _schedules.value = list
            }
        }
    }

    fun autoGenerateWeekend() {
        viewModelScope.launch {
            weeklyPlannerUseCase.regenerateSchedule()
        }
    }
}

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val schedules by viewModel.schedules.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBg)
            .testTag("planner_screen")
    ) {
        WearWiseTopBar(
            title = "WEARWISE",
            subtitle = "AI COUTURE",
            userAvatarRes = WearWiseDrawables.UserAvatar,
            onAvatarClick = onNavigateToProfile
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header (Image 17)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF161A2B))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SCHEDULE MATRIX",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalElectricViolet
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Weekly Style Planner",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Automated outfit scheduling synchronized with your calendar & hyper-local weather forecast.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Week Selector Strip (Image 17)
            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Week", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📅 October 14 – 20",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "WEEK 42 • AUTUMN RESONANCE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = CoutureAmber
                        )
                    }

                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Week", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wardrobe Cohesion Card (Image 17)
            WearWiseCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B2032))
                                .border(2.dp, CoutureEmerald, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("94%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CoutureEmerald)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Wardrobe Cohesion", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Optimal color harmony & zero repeat tops", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    IconButton(onClick = { viewModel.autoGenerateWeekend() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recalculate", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scheduled Days (Mon 14 to Sun 20)
            schedules.forEach { item ->
                DayScheduleCard(
                    item = item,
                    onRegenerate = {
                        Toast.makeText(context, "Regenerating ${item.dayOfWeek} ensemble...", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Laundry Alert (Image 17)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF231F17))
                    .border(1.dp, CoutureAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CoutureAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalLaundryService, contentDescription = null, tint = DarkNavyBg, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Laundry Alert", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("ACTION NEEDED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CoutureAmber)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Wash Oxford shirts by Thursday evening to unlock planned configurations for upcoming lecture series.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Auto-Generate Weekend Button (Image 17)
            WearWiseButton(
                text = "AUTO-GENERATE WEEKEND LOOKS",
                onClick = {
                    viewModel.autoGenerateWeekend()
                    Toast.makeText(context, "Weekend looks generated & synchronized!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_generate_weekend_button"),
                trailingIcon = Icons.Default.AutoAwesome
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayScheduleCard(
    item: WeeklyScheduleItem,
    onRegenerate: () -> Unit
) {
    val borderColor = if (item.isToday) RoyalElectricViolet else CardBorder

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardElevated)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            // Header Row: Day + Title + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (item.isToday) RoyalElectricViolet else Color(0xFF1B2032)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(item.dayOfWeek, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (item.isToday) Color.White else TextMuted)
                            Text("${item.dateDay}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(item.weatherForecast, fontSize = 11.sp, color = TextSecondary)
                    }
                }

                if (item.isWorn) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3310B981))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("✓ WORN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CoutureEmerald)
                    }
                } else if (item.isToday) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(RoyalElectricViolet)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("TODAY'S ENSEMBLE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body preview row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.outfitImageRes != null) {
                    Image(
                        painter = painterResource(id = item.outfitImageRes),
                        contentDescription = item.outfitTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.formalityBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoutureAmber
                    )
                    Text(
                        text = item.outfitTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = item.outfitSubtitle,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                if (!item.isWorn && !item.isUnscheduled) {
                    IconButton(onClick = onRegenerate) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = RoyalElectricViolet, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
