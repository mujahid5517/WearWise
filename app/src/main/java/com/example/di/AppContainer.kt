package com.example.di
import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.DatabaseSeeder
import com.example.data.repository.OutfitRepositoryImpl
import com.example.data.repository.TryOnRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WardrobeRepositoryImpl
import com.example.domain.repository.OutfitRepository
import com.example.domain.repository.TryOnRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WardrobeRepository
import com.example.domain.usecase.ExecuteTryOnUseCase
import com.example.domain.usecase.GetDailyRecommendationUseCase
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.domain.usecase.UserProfileUseCase
import com.example.domain.usecase.WeeklyPlannerUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseSeeder.seedIfNeeded(database)
        }
    }

    val apiKeyManager = com.example.data.local.ApiKeyManager(context)

    val userRepository: UserRepository = UserRepositoryImpl(database)
    val wardrobeRepository: WardrobeRepository = WardrobeRepositoryImpl(database)
    val outfitRepository: OutfitRepository = OutfitRepositoryImpl(database)
    val tryOnRepository: TryOnRepository = TryOnRepositoryImpl(context, database, apiKeyManager)

    val getDailyRecommendationUseCase = GetDailyRecommendationUseCase(outfitRepository)
    val manageWardrobeUseCase = ManageWardrobeUseCase(wardrobeRepository)
    val executeTryOnUseCase = ExecuteTryOnUseCase(tryOnRepository)
    val weeklyPlannerUseCase = WeeklyPlannerUseCase(outfitRepository)
    val userProfileUseCase = UserProfileUseCase(userRepository)
}