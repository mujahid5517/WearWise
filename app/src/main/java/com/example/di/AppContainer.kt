package com.example.di

import android.content.Context
import com.example.data.local.ApiKeyManager
import com.example.data.local.AppDatabase
import com.example.data.local.LanguageManager
import com.example.data.repository.DatabaseSeeder
import com.example.data.repository.OutfitRepositoryImpl
import com.example.data.media.TryOnFileStore
import com.example.data.remote.tryon.CompositeVirtualTryOnProvider
import com.example.data.remote.tryon.FirebaseAiVirtualTryOnProvider
import com.example.data.remote.tryon.GeminiVirtualTryOnProvider
import com.example.data.remote.tryon.TryOnCloudStore
import com.example.data.remote.WardrobeCloudStore
import com.example.data.remote.ShoppingCloudStore
import com.example.data.repository.TryOnRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WardrobeRepositoryImpl
import com.example.data.repository.OpenMeteoWeatherRepository
import com.example.data.repository.ShoppingRepositoryImpl
import com.example.data.sync.ConnectivityMonitor
import com.example.data.sync.SyncManager
import com.example.domain.repository.OutfitRepository
import com.example.domain.repository.TryOnRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WardrobeRepository
import com.example.domain.repository.WeatherRepository
import com.example.domain.repository.ShoppingRepository
import com.example.domain.usecase.ExecuteTryOnUseCase
import com.example.domain.usecase.GetDailyRecommendationUseCase
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.domain.usecase.UserProfileUseCase
import com.example.domain.usecase.WeeklyPlannerUseCase
import com.example.domain.usecase.GetWeatherUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: AppDatabase = AppDatabase.getInstance(context)

    // Firebase Authentication & Cloud Firestore
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Language & Localization
    val languageManager = LanguageManager(context)

    // Connectivity + Sync infrastructure
    val connectivityMonitor = ConnectivityMonitor(context)
    val syncManager = SyncManager(database, connectivityMonitor)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseSeeder.seedIfNeeded(database)
        }
        // Start observing connectivity and processing the sync queue
        syncManager.start()
    }

    val apiKeyManager = ApiKeyManager(context)

    val userRepository: UserRepository = UserRepositoryImpl(database)
    val wardrobeRepository: WardrobeRepository = WardrobeRepositoryImpl(
        database,
        syncManager,
        TryOnFileStore(context),
        WardrobeCloudStore(auth, firestore, FirebaseStorage.getInstance()),
        connectivityMonitor.isOnline
    )
    val outfitRepository: OutfitRepository = OutfitRepositoryImpl(database, syncManager, auth, apiKeyManager)
    val weatherRepository: WeatherRepository = OpenMeteoWeatherRepository(context)
    val shoppingCloudStore = ShoppingCloudStore(auth, firestore)
    val shoppingRepository: ShoppingRepository = ShoppingRepositoryImpl(
        database = database,
        apiKeyManager = apiKeyManager,
        cloudStore = shoppingCloudStore,
        connectivity = connectivityMonitor.isOnline
    )
    val tryOnFileStore = TryOnFileStore(context)
    val tryOnImageProvider = CompositeVirtualTryOnProvider(
        FirebaseAiVirtualTryOnProvider(),
        GeminiVirtualTryOnProvider()
    )
    val tryOnCloudStore = TryOnCloudStore(auth, firestore)
    val tryOnRepository: TryOnRepository = TryOnRepositoryImpl(
        context,
        database,
        apiKeyManager,
        syncManager,
        tryOnFileStore,
        tryOnImageProvider,
        tryOnCloudStore
    )

    val getDailyRecommendationUseCase = GetDailyRecommendationUseCase(outfitRepository)
    val manageWardrobeUseCase = ManageWardrobeUseCase(wardrobeRepository)
    val executeTryOnUseCase = ExecuteTryOnUseCase(tryOnRepository)
    val weeklyPlannerUseCase = WeeklyPlannerUseCase(outfitRepository)
    val getWeatherUseCase = GetWeatherUseCase(weatherRepository)
    val userProfileUseCase = UserProfileUseCase(userRepository)
}
