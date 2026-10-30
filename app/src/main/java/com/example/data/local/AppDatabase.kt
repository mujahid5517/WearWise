package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ClothingItemDao
import com.example.data.local.dao.OutfitDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.dao.TryOnPreviewDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.dao.WeeklyScheduleDao
import com.example.data.local.entity.ClothingItemEntity
import com.example.data.local.entity.OutfitEntity
import com.example.data.local.entity.OutfitItemEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeeklyScheduleEntity

/**
 * Main Room database for WearWise.
 *
 * Version history:
 *  1 → 2: Added sync_queue table for offline-first sync tracking.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        ClothingItemEntity::class,
        OutfitEntity::class,
        OutfitItemEntity::class,
        TryOnPreviewEntity::class,
        WeeklyScheduleEntity::class,
        SyncQueueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun outfitDao(): OutfitDao
    abstract fun tryOnPreviewDao(): TryOnPreviewDao
    abstract fun weeklyScheduleDao(): WeeklyScheduleDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Non-destructive migration from version 1 → 2.
         * Adds the sync_queue table while preserving all existing data.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        operation TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        attemptCount INTEGER NOT NULL DEFAULT 0,
                        lastAttemptAt INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wearwise_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
