package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ClothingItemDao
import com.example.data.local.dao.OutfitDao
import com.example.data.local.dao.TryOnPreviewDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.dao.WeeklyScheduleDao
import com.example.data.local.entity.ClothingItemEntity
import com.example.data.local.entity.OutfitEntity
import com.example.data.local.entity.OutfitItemEntity
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeeklyScheduleEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ClothingItemEntity::class,
        OutfitEntity::class,
        OutfitItemEntity::class,
        TryOnPreviewEntity::class,
        WeeklyScheduleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun outfitDao(): OutfitDao
    abstract fun tryOnPreviewDao(): TryOnPreviewDao
    abstract fun weeklyScheduleDao(): WeeklyScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wearwise_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
