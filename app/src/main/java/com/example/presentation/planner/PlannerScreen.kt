package com.example.presentation.planner

import android.widget.Toast
import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.repository.OutfitRepository
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.domain.model.WeeklyScheduleItem
import com.example.domain.model.WeatherForecast
import com.example.domain.model.WeatherDailyForecast
import com.example.domain.model.WeatherLocation
import com.example.domain.model.WeatherDescriptions
import com.example.domain.model.WearHistory
import com.example.domain.usecase.GetWeatherUseCase
import com.example.domain.usecase.WeatherOutfitSuggestion
import com.example.domain.usecase.WardrobeWeatherRecommender
import com.example.data.notification.OutfitReminderScheduler
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.widget.TimePicker
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
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
import com.example.ui.theme.WarningContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.combine
import com.example.data.local.AppDatabase
import java.time.DayOfWeek
import androidx.compose.foundation.layout.heightIn

class PlannerViewModel(
    private val weeklyPlannerUseCase: WeeklyPlannerUseCase,
    manageWardrobeUseCase: ManageWardrobeUseCase,
    private val outfitRepository: OutfitRepository,
    private val weatherUseCase: GetWeatherUseCase,
    context: Context
) : ViewModel() {

    private val reminderScheduler = OutfitReminderScheduler(context.applicationContext)
    private val _schedules = MutableStateFlow<List<WeeklyScheduleItem>>(emptyList())
    val schedules: StateFlow<List<WeeklyScheduleItem>> = _schedules.asStateFlow()
    val wardrobeItems: StateFlow<List<ClothingItem>> = manageWardrobeUseCase.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val outfits: StateFlow<List<Outfit>> = outfitRepository.getAllOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val wearHistory: StateFlow<List<WearHistory>> = outfitRepository.getWearHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    private val _weather = MutableStateFlow<WeatherForecast?>(null)
    val weather: StateFlow<WeatherForecast?> = _weather.asStateFlow()
    private val _weatherLoading = MutableStateFlow(false)
    val weatherLoading: StateFlow<Boolean> = _weatherLoading.asStateFlow()
    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()
    private val _location = MutableStateFlow(weatherUseCase.savedLocation())
    val location: StateFlow<WeatherLocation?> = _location.asStateFlow()
    private val _cityResults = MutableStateFlow<List<WeatherLocation>>(emptyList())
    val cityResults: StateFlow<List<WeatherLocation>> = _cityResults.asStateFlow()
    private val _citySearchLoading = MutableStateFlow(false)
    val citySearchLoading: StateFlow<Boolean> = _citySearchLoading.asStateFlow()
    private val _citySearchError = MutableStateFlow<String?>(null)
    val citySearchError: StateFlow<String?> = _citySearchError.asStateFlow()
    private val _variation = MutableStateFlow(0)
    val recommendation = combine(_weather, wardrobeItems, wearHistory, _variation, _selectedDate) { forecast, clothes, history, variation, date ->
        if (forecast == null) null else {
            val day = forecast.daily.firstOrNull { it.date == date.toString() }
            if (day == null) return@combine null
            val conditions = forecast.copy(
                temperatureC = day.highC ?: forecast.temperatureC,
                feelsLikeC = day.apparentHighC ?: day.highC ?: forecast.feelsLikeC,
                precipitationProbability = day.precipitationProbability ?: forecast.precipitationProbability,
                precipitationMm = if (date == LocalDate.now()) forecast.precipitationMm else 0.0,
                windKmh = day.maxWindKmh ?: forecast.windKmh,
                weatherCode = day.weatherCode ?: forecast.weatherCode
            )
            WardrobeWeatherRecommender.recommend(conditions, clothes, history, variation)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _aiWeatherOutfit = MutableStateFlow<Outfit?>(null)
    val aiWeatherOutfit: StateFlow<Outfit?> = _aiWeatherOutfit.asStateFlow()
    private val _aiWeatherLoading = MutableStateFlow(false)
    val aiWeatherLoading: StateFlow<Boolean> = _aiWeatherLoading.asStateFlow()
    private val _aiWeatherError = MutableStateFlow<String?>(null)
    val aiWeatherError: StateFlow<String?> = _aiWeatherError.asStateFlow()

    init {
        loadSchedule()
        viewModelScope.launch {
            val saved = _location.value
            _weather.value = weatherUseCase.cached()?.takeIf { cached ->
                saved == null || (cached.location.latitude == saved.latitude && cached.location.longitude == saved.longitude)
            }
            saved?.let { refreshWeather(it, false) }
        }
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            weeklyPlannerUseCase.getSchedule().collect { list ->
                _schedules.value = list
            }
        }
    }

    fun autoGenerateWeekend() {
        _variation.value++
    }

    fun moveWeek(weeks: Long) { _selectedDate.value = _selectedDate.value.plusWeeks(weeks) }
    fun selectDate(date: LocalDate) { _selectedDate.value = date; _aiWeatherOutfit.value = null; _aiWeatherError.value = null }
    fun generateAnother() { _aiWeatherOutfit.value = null; _variation.value++ }

    fun refreshWeather(location: WeatherLocation? = _location.value, force: Boolean = true) {
        if (location == null) {
            _weatherError.value = "Choose a city or use your current location to see live weather."
            return
        }
        if (_weather.value?.location?.latitude != location.latitude || _weather.value?.location?.longitude != location.longitude) _weather.value = null
        viewModelScope.launch {
            _aiWeatherOutfit.value = null
            _weatherLoading.value = true
            _weatherError.value = null
            try {
                _weather.value = weatherUseCase.refresh(location, force)
                val age = System.currentTimeMillis() - (_weather.value?.fetchedAtEpochMillis ?: 0L)
                if (age > WEATHER_STALE_AFTER_MS) _weatherError.value = "Offline: showing the last saved weather update."
            } catch (error: Exception) {
                _weatherError.value = if (_weather.value == null) {
                    "Weather could not be loaded. Check your connection and try again."
                } else "Offline: showing the last weather update. ${error.message.orEmpty()}"
            } finally { _weatherLoading.value = false }
        }
    }

    fun useLocation(location: WeatherLocation) {
        _aiWeatherOutfit.value = null
        _location.value = location
        weatherUseCase.saveLocation(location)
        refreshWeather(location, true)
    }

    fun searchCities(query: String) {
        viewModelScope.launch {
            _citySearchLoading.value = true
            _citySearchError.value = null
            try {
                _cityResults.value = weatherUseCase.search(query)
                if (_cityResults.value.isEmpty()) _citySearchError.value = "No matching city found. Try adding a region or country."
            } catch (error: Exception) {
                _citySearchError.value = "Could not search locations. Check your connection."
                _cityResults.value = emptyList()
            } finally { _citySearchLoading.value = false }
        }
    }

    fun clearCitySearch() { _cityResults.value = emptyList(); _citySearchError.value = null }

    fun saveWardrobeOutfit(
        title: String,
        occasion: String,
        items: List<ClothingItem>,
        date: LocalDate? = null,
        reminderEnabled: Boolean = false,
        reminderMinutes: Int = 480,
        onSaved: (Outfit) -> Unit
    ) {
        if (items.isEmpty() || title.isBlank()) return
        viewModelScope.launch {
            try {
                val outfit = Outfit(
                title = title.trim(), occasion = occasion, style = "Personal wardrobe",
                rationale = "Created from items in your WearWise wardrobe.", items = items
                )
                val id = outfitRepository.saveOutfit(outfit)
                val savedOutfit = outfit.copy(id = id)
                date?.let { scheduleOutfit(savedOutfit, it, occasion, reminderEnabled, reminderMinutes) }
                onSaved(savedOutfit)
            } catch (error: Exception) {
                _error.value = error.message ?: "Could not save this outfit plan."
            }
        }
    }

    fun saveSuggestionAsWorn(items: List<ClothingItem>, occasion: String, onSaved: (Outfit) -> Unit) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            try {
                val outfit = Outfit(
                    title = "Weather-ready ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))}",
                    occasion = occasion,
                    style = "Weather-ready",
                    rationale = "Selected from your wardrobe for today's live forecast.",
                    items = items
                )
                val id = outfitRepository.saveOutfit(outfit)
                val saved = outfit.copy(id = id)
                outfitRepository.recordOutfitWorn(id)
                onSaved(saved)
            } catch (error: Exception) {
                _error.value = error.message ?: "Could not record the outfit as worn."
            }
        }
    }

    fun planExistingOutfit(outfit: Outfit, date: LocalDate = _selectedDate.value, reminderEnabled: Boolean = false, reminderMinutes: Int = 480) {
        viewModelScope.launch {
            runCatching { scheduleOutfit(outfit, date, outfit.occasion, reminderEnabled, reminderMinutes) }
                .onFailure { _error.value = it.message ?: "Could not schedule this outfit." }
        }
    }

    private suspend fun scheduleOutfit(outfit: Outfit, date: LocalDate, occasion: String, reminderEnabled: Boolean, reminderMinutes: Int) {
        val forecast = _weather.value?.daily?.firstOrNull { it.date == date.toString() }
        val item = WeeklyScheduleItem(
            dayOfWeek = date.dayOfWeek.name.take(3), dateDay = date.dayOfMonth,
            title = occasion, weatherForecast = forecast?.let(::dailyWeatherSummary) ?: "Weather forecast unavailable",
            formalityBadge = "Planned", outfitTitle = outfit.title,
            outfitSubtitle = outfit.items.joinToString(" · ") { it.name },
            outfitImageRes = outfit.items.firstOrNull { it.imageRes != null }?.imageRes,
            isToday = date == LocalDate.now(), isWorn = false, isUnscheduled = false,
            dateIso = date.toString(), outfitId = outfit.id, occasion = occasion,
            reminderEnabled = reminderEnabled, reminderTimeMinutes = reminderMinutes,
            notes = "", ownerUid = outfit.ownerUid
        )
        val id = weeklyPlannerUseCase.saveScheduleItem(item)
        reminderScheduler.schedule(item.copy(id = id))
    }

    fun deletePlan(item: WeeklyScheduleItem) {
        viewModelScope.launch {
            runCatching { weeklyPlannerUseCase.deleteScheduleItem(item.id) }
                .onSuccess { reminderScheduler.cancel(item.dateIso) }
                .onFailure { _error.value = it.message ?: "Could not remove this plan." }
        }
    }

    fun markWorn(item: WeeklyScheduleItem) {
        val outfitId = item.outfitId ?: return
        viewModelScope.launch {
            try {
                outfitRepository.recordOutfitWorn(outfitId)
                weeklyPlannerUseCase.updateScheduleItem(item.copy(isWorn = true, wornAtEpochMillis = System.currentTimeMillis()))
            } catch (error: Exception) {
                _error.value = error.message ?: "Could not record this outfit as worn."
            }
        }
    }

    fun clearError() { _error.value = null }

    fun generateAiWeatherOutfit(occasion: String = "Everyday") {
        val forecast = _weather.value ?: run { _aiWeatherError.value = "Load weather before asking AI."; return }
        val date = _selectedDate.value
        val day = forecast.daily.firstOrNull { it.date == date.toString() } ?: run {
            _aiWeatherError.value = "AI weather styling is available for dates in the current forecast."
            return
        }
        viewModelScope.launch {
            _aiWeatherLoading.value = true
            _aiWeatherError.value = null
            try {
                val selectedConditions = forecast.copy(
                    temperatureC = day.highC ?: forecast.temperatureC,
                    feelsLikeC = day.apparentHighC ?: day.highC ?: forecast.feelsLikeC,
                    precipitationProbability = day.precipitationProbability ?: forecast.precipitationProbability,
                    precipitationMm = if (date == LocalDate.now()) forecast.precipitationMm else 0.0,
                    windKmh = day.maxWindKmh ?: forecast.windKmh,
                    weatherCode = day.weatherCode ?: forecast.weatherCode
                )
                _aiWeatherOutfit.value = outfitRepository.generateWeatherRecommendation(selectedConditions, occasion)
                if (_aiWeatherOutfit.value == null) _aiWeatherError.value = "AI styling is unavailable. Add a Gemini API key in Profile; the local weather recommendation still works."
            } catch (error: Exception) {
                _aiWeatherError.value = error.message ?: "AI styling failed. The local weather recommendation is still available."
            } finally { _aiWeatherLoading.value = false }
        }
    }

    companion object { private const val WEATHER_STALE_AFTER_MS = 30 * 60 * 1000L }
}

@Composable
private fun LegacyPlannerScreen(
    viewModel: PlannerViewModel,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val schedules by viewModel.schedules.collectAsState()
    val wardrobeItems by viewModel.wardrobeItems.collectAsState()
    var showWardrobeOutfitDialog by remember { mutableStateOf(false) }

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
                    .background(CardElevated)
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

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showWardrobeOutfitDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet)
            ) { Text("Create outfit from my wardrobe") }

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
                                .background(CardElevated)
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
                    .background(WarningContainer)
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

    if (showWardrobeOutfitDialog) {
        WardrobeOutfitDialog(
            items = wardrobeItems,
            onDismiss = { showWardrobeOutfitDialog = false },
            onSave = { title, occasion, selected ->
                viewModel.saveWardrobeOutfit(title, occasion, selected) { _ ->
                    showWardrobeOutfitDialog = false
                    Toast.makeText(context, "Outfit saved with your wardrobe items", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun WardrobeOutfitDialog(
    items: List<ClothingItem>,
    onDismiss: () -> Unit,
    onSave: (String, String, List<ClothingItem>) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var occasion by remember { mutableStateOf("Everyday") }
    val selected = remember { mutableStateListOf<Long>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create from your wardrobe") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(title, { title = it }, label = { Text("Outfit name") }, singleLine = true)
                OutlinedTextField(occasion, { occasion = it }, label = { Text("Occasion") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                if (items.isEmpty()) Text("Add clothing to your wardrobe first.") else LazyColumn(Modifier.height(340.dp)) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (item.id in selected) selected.remove(item.id) else selected.add(item.id)
                            }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val image = item.imageUri ?: item.imageUrl ?: item.imageRes ?: item.catalogAssetName?.let {
                                context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 }
                            }
                            if (image != null) AsyncImage(image, item.name, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("${item.category} · ${item.color}", fontSize = 11.sp, color = TextSecondary)
                            }
                            Checkbox(checked = item.id in selected, onCheckedChange = { checked ->
                                if (checked) selected.add(item.id) else selected.remove(item.id)
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, occasion, items.filter { it.id in selected }) }, enabled = title.isNotBlank() && selected.isNotEmpty()) { Text("Save outfit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
                            .background(if (item.isToday) RoyalElectricViolet else CardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(item.dayOfWeek, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (item.isToday) Color.White else TextMuted)
                            Text("${item.dateDay}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (item.isToday) Color.White else TextPrimary)
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

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    onNavigateToProfile: () -> Unit,
    onUseInTryOn: (Outfit) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val schedules by viewModel.schedules.collectAsState()
    val wardrobe by viewModel.wardrobeItems.collectAsState()
    val savedOutfits by viewModel.outfits.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val loading by viewModel.weatherLoading.collectAsState()
    val weatherError by viewModel.weatherError.collectAsState()
    val location by viewModel.location.collectAsState()
    val suggestion by viewModel.recommendation.collectAsState()
    val aiWeatherOutfit by viewModel.aiWeatherOutfit.collectAsState()
    val aiWeatherLoading by viewModel.aiWeatherLoading.collectAsState()
    val aiWeatherError by viewModel.aiWeatherError.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val history by viewModel.wearHistory.collectAsState()
    val error by viewModel.error.collectAsState()
    var planDialog by remember { mutableStateOf(false) }
    var cityDialog by remember { mutableStateOf(false) }
    var initialItems by remember { mutableStateOf(emptyList<ClothingItem>()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) requestPlannerLocation(context, viewModel)
        else Toast.makeText(context, "Choose a city manually to see its weather.", Toast.LENGTH_LONG).show()
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "Notifications are disabled. You can enable them in Android Settings.", Toast.LENGTH_LONG).show()
    }
    val requestLocation = {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) requestPlannerLocation(context, viewModel)
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    androidx.compose.runtime.LaunchedEffect(lifecycleOwner, location) {
        val selectedLocation = location ?: return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                delay(30 * 60 * 1000L)
                viewModel.refreshWeather(selectedLocation, force = false)
            }
        }
    }
    val weekStart = selectedDate.with(DayOfWeek.MONDAY)
    val days = (0L..6L).map { weekStart.plusDays(it) }
    val plansByDate = schedules.filter { it.dateIso.isNotBlank() }.associateBy { it.dateIso }
    val selectedPlan = plansByDate[selectedDate.toString()]
    val outfit = selectedPlan?.outfitId?.let { id -> savedOutfits.firstOrNull { it.id == id } }
    val selectedForecast = weather?.daily?.firstOrNull { it.date == selectedDate.toString() }
    val visibleSuggestion = aiWeatherOutfit?.let { ai ->
        WeatherOutfitSuggestion(
            items = ai.items,
            reason = "AI selected this combination for ${ai.occasion.lowercase()}: ${ai.rationale}",
            conditionSummary = selectedForecast?.let(::dailyWeatherSummary) ?: suggestion?.conditionSummary.orEmpty()
        )
    } ?: suggestion

    Column(Modifier.fillMaxSize().background(DarkNavyBg).testTag("planner_screen")) {
        WearWiseTopBar(title = "WEARWISE", subtitle = "WEEKLY STYLE PLANNER", userAvatarRes = WearWiseDrawables.UserAvatar, onAvatarClick = onNavigateToProfile)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text("Plan your week", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())), color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            WeatherDashboardCard(weather, location, loading, weatherError,
                onRefresh = { viewModel.refreshWeather(force = true) },
                onChooseCity = { cityDialog = true }, onUseLocation = requestLocation)
            Spacer(Modifier.height(15.dp))
            WearWiseCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { viewModel.moveWeek(-1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous week", tint = TextSecondary) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${weekStart.format(DateTimeFormatter.ofPattern("d MMM"))} – ${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM yyyy"))}", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${days.count { plansByDate.containsKey(it.toString()) }} planned outfits", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = { viewModel.moveWeek(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next week", tint = TextSecondary) }
                }
            }
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { date ->
                    val isSelected = selectedDate == date
                    val dayForecast = weather?.daily?.firstOrNull { it.date == date.toString() }
                    val isPlanned = date.toString() in plansByDate
                    Column(
                        Modifier.clip(RoundedCornerShape(15.dp)).background(if (isSelected) RoyalElectricViolet else CardElevated)
                            .clickable { viewModel.selectDate(date) }.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(date.dayOfWeek.name.take(3), color = if (isSelected) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(date.dayOfMonth.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(dayForecast?.highC?.let { "$it°" } ?: "—", color = if (isSelected) Color.White else TextSecondary, fontSize = 10.sp)
                        Text(if (isPlanned) "●" else "○", color = if (isPlanned) CoutureAmber else TextMuted, fontSize = 9.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(selectedForecast?.let { "${WeatherDescriptions.describe(it.weatherCode ?: 0)} · high ${it.highC ?: "–"}° / low ${it.lowC ?: "–"}° · rain ${it.precipitationProbability ?: "–"}%" } ?: "No forecast available for this date", color = TextSecondary, fontSize = 11.sp)
                }
                IconButton(onClick = { initialItems = outfit?.items.orEmpty(); planDialog = true }) {
                    Icon(if (selectedPlan == null) Icons.Default.AutoAwesome else Icons.Default.Edit, "Add or edit outfit plan", tint = CoutureAmber)
                }
            }
            val latestDaySummary = selectedForecast?.let(::dailyWeatherSummary)
            if (selectedPlan != null && latestDaySummary != null && latestDaySummary != selectedPlan.weatherForecast) {
                WearWiseCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Forecast changed since this outfit was planned", color = CoutureAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${selectedPlan.weatherForecast} → $latestDaySummary. Your outfit has not been changed.", color = TextSecondary, fontSize = 11.sp)
                        TextButton(onClick = { initialItems = suggestion?.items.orEmpty(); planDialog = true }) { Text("Review weather-ready outfit") }
                    }
                }
            }
            if (selectedPlan == null) {
                WearWiseCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No outfit planned for this day", color = TextSecondary)
                        Button(onClick = { initialItems = suggestion?.items.orEmpty(); planDialog = true }, enabled = wardrobe.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = RoyalElectricViolet)) { Text("Plan from my wardrobe") }
                    }
                }
            } else {
                PlannedOutfitCard(
                    selectedPlan, outfit,
                    onEdit = { initialItems = outfit?.items.orEmpty(); planDialog = true },
                    onDelete = { viewModel.deletePlan(selectedPlan) },
                    onWear = { viewModel.markWorn(selectedPlan) },
                    onTryOn = { outfit?.let(onUseInTryOn) }
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (selectedDate == LocalDate.now()) "Recommended outfit for today" else "Weather look for ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.generateAnother() }, enabled = visibleSuggestion != null) { Icon(Icons.Default.Refresh, "Generate another", tint = CoutureAmber) }
            }
            if (visibleSuggestion == null) {
                WearWiseCard(Modifier.fillMaxWidth()) {
                    val emptyMessage = when {
                        weather == null -> "Choose a location to get a live weather-based look."
                        wardrobe.isEmpty() -> "Add clothing to your wardrobe to get suggestions using items you own."
                        else -> "A weather recommendation is not available yet. Pull to refresh weather."
                    }
                    Text(emptyMessage, color = TextSecondary, modifier = Modifier.padding(12.dp))
                }
            } else {
                WeatherRecommendationCard(
                    visibleSuggestion!!,
                    aiLoading = aiWeatherLoading,
                    aiActive = aiWeatherOutfit != null,
                    onAskAi = { viewModel.generateAiWeatherOutfit() },
                    onAddToPlan = { initialItems = visibleSuggestion!!.items; planDialog = true },
                    onSave = { viewModel.saveWardrobeOutfit("Weather-ready outfit", "Everyday", visibleSuggestion!!.items) { Toast.makeText(context, "Outfit saved", Toast.LENGTH_SHORT).show() } },
                    onWear = {
                        if (selectedDate == LocalDate.now()) viewModel.saveSuggestionAsWorn(visibleSuggestion!!.items, "Everyday") { Toast.makeText(context, "Wear history updated", Toast.LENGTH_SHORT).show() }
                        else Toast.makeText(context, "Select today to record an outfit as worn.", Toast.LENGTH_SHORT).show()
                    },
                    onTryOn = { onUseInTryOn(Outfit(title = "Weather-ready outfit", occasion = "Everyday", style = "Weather-ready", rationale = visibleSuggestion!!.reason, items = visibleSuggestion!!.items)) }
                )
            }
            if (!aiWeatherError.isNullOrBlank()) Text(aiWeatherError!!, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("Recently worn", color = TextPrimary, fontWeight = FontWeight.Bold)
                history.take(3).forEach { entry ->
                    val date = java.time.Instant.ofEpochMilli(entry.wornAtEpochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    Text("${entry.outfitTitle} · $date", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            if (!error.isNullOrBlank()) Text(error!!, color = Color(0xFFFF928C), modifier = Modifier.clickable { viewModel.clearError() }.padding(vertical = 8.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
    if (planDialog) {
        PlanOutfitDialog(
            items = wardrobe, initialItems = initialItems, date = selectedDate,
            onRequestNotificationPermission = {
                if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = { planDialog = false },
            onSave = { title, occasion, chosen, reminder, minutes ->
                viewModel.saveWardrobeOutfit(title, occasion, chosen, date = selectedDate, reminderEnabled = reminder, reminderMinutes = minutes) {
                    planDialog = false
                    Toast.makeText(context, "Outfit planned for ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    if (cityDialog) CitySearchDialog(viewModel, onDismiss = { cityDialog = false }, onChoose = { city -> cityDialog = false; viewModel.useLocation(city) })
}

private fun dailyWeatherSummary(day: WeatherDailyForecast): String =
    "${WeatherDescriptions.describe(day.weatherCode ?: 0)} · ${day.highC ?: "–"}° / ${day.lowC ?: "–"}° · rain ${day.precipitationProbability ?: "–"}%"

private fun requestPlannerLocation(context: Context, viewModel: PlannerViewModel) {
    val token = CancellationTokenSource()
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gpsEnabled = runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
    val networkEnabled = runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
    if (!gpsEnabled && !networkEnabled) {
        Toast.makeText(context, "Location is turned off. Enable it or choose a city manually.", Toast.LENGTH_LONG).show()
        token.cancel()
        return
    }
    LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
        .addOnSuccessListener { loc ->
            if (loc == null) Toast.makeText(context, "Could not get a location fix. Choose a city manually.", Toast.LENGTH_LONG).show()
            else viewModel.useLocation(WeatherLocation(latitude = loc.latitude, longitude = loc.longitude, name = "Current location"))
        }
        .addOnFailureListener { Toast.makeText(context, "Location unavailable. Choose a city manually.", Toast.LENGTH_LONG).show() }
}

@Composable
private fun WeatherDashboardCard(
    weather: WeatherForecast?, location: WeatherLocation?, loading: Boolean, error: String?,
    onRefresh: () -> Unit, onChooseCity: () -> Unit, onUseLocation: () -> Unit
) {
    WearWiseCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("LIVE WEATHER", color = CoutureAmber, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text(weather?.location?.displayName ?: location?.displayName ?: "Set your weather location", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onRefresh, enabled = !loading) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = CoutureAmber, strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, "Refresh weather", tint = TextSecondary)
                }
            }
            if (weather == null) {
                Text("Weather uses a one-time location or a city you choose. Location is not tracked in the background.", color = TextSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onUseLocation) { Icon(Icons.Default.MyLocation, null); Text("Use my location") }
                    TextButton(onClick = onChooseCity) { Icon(Icons.Default.LocationOn, null); Text("Choose city") }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(weatherEmoji(weather.weatherCode), fontSize = 36.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${weather.temperatureC?.toInt() ?: "–"}°C", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                        Text(WeatherDescriptions.describe(weather.weatherCode), color = TextSecondary, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Feels ${weather.feelsLikeC?.toInt() ?: "–"}°", color = TextSecondary, fontSize = 11.sp)
                        Text("Humidity ${weather.humidityPercent?.let { "$it%" } ?: "–"}", color = TextSecondary, fontSize = 11.sp)
                        Text("Wind ${weather.windKmh?.toInt()?.let { "$it km/h" } ?: "–"}", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                weather.daily.firstOrNull()?.let { today ->
                    Text("Today · high ${today.highC?.toInt() ?: "–"}° / low ${today.lowC?.toInt() ?: "–"}° · rainfall ${weather.precipitationMm?.let { "${it} mm" } ?: "–"} · rain chance ${weather.precipitationProbability?.let { "$it%" } ?: "–"}", color = TextSecondary, fontSize = 11.sp)
                }
                if (weather.hourly.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp)); Text("HOURLY", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(weather.hourly.filter { it.time >= LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH")) }.take(12)) { hour ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(hour.time.takeLast(5), color = TextMuted, fontSize = 9.sp)
                                Text(weatherEmoji(hour.weatherCode), fontSize = 16.sp)
                                Text("${hour.temperatureC?.toInt() ?: "–"}°", color = TextPrimary, fontSize = 10.sp)
                                Text("${hour.precipitationProbability?.let { "$it%" } ?: "–"}", color = TextMuted, fontSize = 8.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp)); Text("7-DAY FORECAST", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(weather.daily) { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(runCatching { LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())) }.getOrDefault(day.date), color = TextMuted, fontSize = 9.sp)
                            Text(weatherEmoji(day.weatherCode), fontSize = 16.sp)
                            Text("${day.highC?.toInt() ?: "–"}° / ${day.lowC?.toInt() ?: "–"}°", color = TextPrimary, fontSize = 9.sp)
                            Text("${day.precipitationProbability?.let { "$it%" } ?: "–"} rain", color = TextMuted, fontSize = 8.sp)
                        }
                    }
                }
                Text("Updated ${java.time.Instant.ofEpochMilli(weather.fetchedAtEpochMillis).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))} · Weather data by Open-Meteo.com", color = TextMuted, fontSize = 9.sp)
                if (!error.isNullOrBlank()) Text(error, color = CoutureAmber, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onUseLocation) { Text("Use current location", fontSize = 10.sp) }
                    TextButton(onClick = onChooseCity) { Text("Change city", fontSize = 10.sp) }
                }
            }
        }
    }
}

private fun weatherEmoji(code: Int?): String = when (code) {
    0 -> "\u2600\uFE0F"
    1, 2 -> "\uD83C\uDF24\uFE0F"
    3 -> "\u2601\uFE0F"
    45, 48 -> "\uD83C\uDF2B\uFE0F"
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "\uD83C\uDF27\uFE0F"
    71, 73, 75, 77, 85, 86 -> "\u2744\uFE0F"
    95, 96, 99 -> "\u26C8\uFE0F"
    else -> "\u2601\uFE0F"
}

@Composable
private fun WeatherRecommendationCard(
    suggestion: WeatherOutfitSuggestion,
    aiLoading: Boolean,
    aiActive: Boolean,
    onAskAi: () -> Unit,
    onAddToPlan: () -> Unit,
    onSave: () -> Unit,
    onWear: () -> Unit,
    onTryOn: () -> Unit
) {
    WearWiseCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(suggestion.conditionSummary, color = CoutureAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 9.dp)) {
                items(suggestion.items) { item ->
                    Column(Modifier.width(78.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val context = LocalContext.current
                        val image: Any? = item.imageUri ?: item.imageUrl ?: item.imageRes ?: item.catalogAssetName?.let { context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 } }
                        if (image != null) AsyncImage(image, item.name, Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Text(item.name, color = TextPrimary, fontSize = 9.sp, maxLines = 2)
                        Text(item.category, color = TextMuted, fontSize = 8.sp)
                    }
                }
            }
            Text(suggestion.reason, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
            Text("Optional AI styling sends clothing names, categories, colors and forecast details to Gemini.", color = TextMuted, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = onAddToPlan) { Text("Add to Planner") }
                TextButton(onClick = onAskAi, enabled = !aiLoading) { Text(if (aiLoading) "AI styling…" else if (aiActive) "AI look ready" else "Ask AI") }
                TextButton(onClick = onWear) { Text("Wear Today") }
                TextButton(onClick = onSave) { Text("Save Outfit") }
                TextButton(onClick = onTryOn) { Text("Try On") }
            }
        }
    }
}

@Composable
private fun PlannedOutfitCard(
    plan: WeeklyScheduleItem,
    outfit: Outfit?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWear: () -> Unit,
    onTryOn: () -> Unit
) {
    WearWiseCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(plan.outfitTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${plan.occasion} · ${plan.weatherForecast}", color = TextSecondary, fontSize = 11.sp)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit plan", tint = CoutureAmber) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Remove plan", tint = Color(0xFFFF928C)) }
            }
            if (!outfit?.items.isNullOrEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(outfit!!.items) { item ->
                        val context = LocalContext.current
                        val image: Any? = item.imageUri ?: item.imageUrl ?: item.imageRes ?: item.catalogAssetName?.let { context.resources.getIdentifier(it, "drawable", context.packageName).takeIf { id -> id != 0 } }
                        if (image != null) AsyncImage(image, item.name, Modifier.size(55.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    }
                }
            } else Text(plan.outfitSubtitle.ifBlank { "Outfit details unavailable" }, color = TextSecondary, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onWear, enabled = !plan.isWorn && plan.outfitId != null, colors = ButtonDefaults.buttonColors(containerColor = CoutureEmerald)) { Text(if (plan.isWorn) "Worn ✓" else "Wear Today") }
                TextButton(onClick = onTryOn, enabled = outfit != null) { Text("Try On") }
                if (plan.reminderEnabled) Text("🔔 ${plan.reminderTimeMinutes / 60}:${(plan.reminderTimeMinutes % 60).toString().padStart(2, '0')}", color = TextSecondary, modifier = Modifier.padding(top = 12.dp), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PlanOutfitDialog(
    items: List<ClothingItem>, initialItems: List<ClothingItem>, date: LocalDate,
    onRequestNotificationPermission: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, List<ClothingItem>, Boolean, Int) -> Unit
) {
    val context = LocalContext.current
    var title by remember(date, initialItems) { mutableStateOf(initialItems.takeIf { it.isNotEmpty() }?.joinToString(" + ") { it.name } ?: "") }
    var occasion by remember(date) { mutableStateOf("Everyday") }
    var reminder by remember(date) { mutableStateOf(false) }
    var reminderMinutes by remember(date) { mutableStateOf(8 * 60) }
    val selected = remember(date, initialItems) { mutableStateListOf<Long>().apply { addAll(initialItems.map { it.id }) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan for ${date.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))}") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(title, { title = it }, label = { Text("Outfit name") }, singleLine = true)
                OutlinedTextField(occasion, { occasion = it }, label = { Text("Occasion") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminder, onCheckedChange = { reminder = it; if (it) onRequestNotificationPermission() })
                    Text("Remind me", color = TextPrimary)
                    TextButton(onClick = {
                        TimePickerDialog(context, { _, hour, minute -> reminderMinutes = hour * 60 + minute }, reminderMinutes / 60, reminderMinutes % 60, false).show()
                    }, enabled = reminder) { Text("${(reminderMinutes / 60).toString().padStart(2, '0')}:${(reminderMinutes % 60).toString().padStart(2, '0')}") }
                }
                if (items.isEmpty()) Text("Add clothing to your wardrobe first.", color = TextSecondary)
                else LazyColumn(Modifier.height(260.dp)) {
                    items(items, key = { it.id }) { item ->
                        Row(Modifier.fillMaxWidth().clickable {
                            if (item.id in selected) selected.remove(item.id) else selected.add(item.id)
                            if (title.isBlank()) title = item.name
                        }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val image = item.imageUri ?: item.imageUrl ?: item.imageRes
                            if (image != null) AsyncImage(image, item.name, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("${item.category} · ${item.color}", color = TextSecondary, fontSize = 10.sp)
                            }
                            Checkbox(checked = item.id in selected, onCheckedChange = { checked -> if (checked) selected.add(item.id) else selected.remove(item.id) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, occasion, items.filter { it.id in selected }, reminder, reminderMinutes) }, enabled = title.isNotBlank() && occasion.isNotBlank() && selected.isNotEmpty()) { Text("Save plan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CitySearchDialog(viewModel: PlannerViewModel, onDismiss: () -> Unit, onChoose: (WeatherLocation) -> Unit) {
    val results by viewModel.cityResults.collectAsState()
    val loading by viewModel.citySearchLoading.collectAsState()
    val error by viewModel.citySearchError.collectAsState()
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { viewModel.clearCitySearch(); onDismiss() },
        title = { Text("Choose a city") },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, label = { Text("City, region or country") }, singleLine = true)
                Button(onClick = { viewModel.searchCities(query) }, enabled = query.trim().length >= 2 && !loading) { Text(if (loading) "Searching…" else "Search") }
                if (!error.isNullOrBlank()) Text(error!!, color = TextSecondary, fontSize = 11.sp)
                LazyColumn(Modifier.heightIn(max = 250.dp)) {
                    items(results) { city -> TextButton(onClick = { viewModel.clearCitySearch(); onChoose(city) }) { Text(city.displayName, color = TextPrimary) } }
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.clearCitySearch(); onDismiss() }) { Text("Close") } }
    )
}
