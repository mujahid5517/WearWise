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
import com.example.data.local.dao.WearHistoryDao
import com.example.data.local.dao.ShoppingItemDao
import com.example.data.local.dao.OutfitFeedbackDao
import com.example.data.local.entity.ClothingItemEntity
import com.example.data.local.entity.OutfitEntity
import com.example.data.local.entity.OutfitItemEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeeklyScheduleEntity
import com.example.data.local.entity.WearHistoryEntity
import com.example.data.local.entity.ShoppingItemEntity
import com.example.data.local.entity.TripEntity
import com.example.data.local.dao.TripDao
import com.example.data.local.entity.OutfitFeedbackEntity

/**
 * Main Room database for WearWise.
 *
 * Version history:
 *  1 → 2: Added sync_queue table for offline-first sync tracking.
 *  2 → 3: Try-on saved looks and garment summary columns.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        ClothingItemEntity::class,
        OutfitEntity::class,
        OutfitItemEntity::class,
        TryOnPreviewEntity::class,
        WeeklyScheduleEntity::class,
        SyncQueueEntity::class,
        WearHistoryEntity::class,
        ShoppingItemEntity::class,
        TripEntity::class
    ],
    version = 12,
        OutfitFeedbackEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun clothingItemDao(): ClothingItemDao
    abstract fun outfitDao(): OutfitDao
    abstract fun tryOnPreviewDao(): TryOnPreviewDao
    abstract fun weeklyScheduleDao(): WeeklyScheduleDao
    abstract fun wearHistoryDao(): WearHistoryDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun tripDao(): TripDao
    abstract fun outfitFeedbackDao(): OutfitFeedbackDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE try_on_previews ADD COLUMN isSavedLook INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE try_on_previews ADD COLUMN garmentSummary TEXT"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN brand TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN size TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN occasion TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN imageStoragePath TEXT")
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN pattern TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE outfits ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clothing_items ADD COLUMN catalogAssetName TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE weekly_schedules ADD COLUMN dateIso TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE weekly_schedules ADD COLUMN outfitId INTEGER")
                db.execSQL("ALTER TABLE weekly_schedules ADD COLUMN occasion TEXT NOT NULL DEFAULT 'Everyday'")
                db.execSQL("ALTER TABLE weekly_schedules ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE weekly_schedules ADD COLUMN reminderTimeMinutes INTEGER NOT NULL DEFAULT 480")
                db.execSQL("ALTER TABLE weekly_schedules ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE weekly_schedules ADD COLUMN wornAtEpochMillis INTEGER")
                db.execSQL("ALTER TABLE weekly_schedules ADD COLUMN ownerUid TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS wear_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    outfitId INTEGER NOT NULL,
                    outfitTitle TEXT NOT NULL,
                    clothingItemIds TEXT NOT NULL,
                    wornAtEpochMillis INTEGER NOT NULL,
                    occasion TEXT NOT NULL,
                    ownerUid TEXT NOT NULL DEFAULT ''
                )""".trimIndent())
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS shopping_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    remoteId TEXT,
                    ownerUid TEXT NOT NULL DEFAULT '',
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    estimatedPrice REAL NOT NULL DEFAULT 0.0,
                    currency TEXT NOT NULL DEFAULT 'USD',
                    shoppingUrl TEXT NOT NULL DEFAULT '',
                    reason TEXT NOT NULL DEFAULT '',
                    priority TEXT NOT NULL DEFAULT 'MEDIUM',
                    status TEXT NOT NULL DEFAULT 'NEEDED',
                    favorite INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )""".trimIndent())
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN colorSeason TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN skinUndertone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN hairColorAnalyzed TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN eyeColorAnalyzed TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN recommendedColors TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN colorsToAvoid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN colorAnalysisRationale TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN colorAnalysisPhotoPath TEXT")
                db.execSQL("""CREATE TABLE IF NOT EXISTS outfit_feedback (
                    feedbackId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    ownerUid TEXT NOT NULL, outfitId INTEGER NOT NULL, rating INTEGER,
                    reaction TEXT, comment TEXT NOT NULL, outfitTitle TEXT NOT NULL,
                    occasion TEXT NOT NULL, style TEXT NOT NULL, colors TEXT NOT NULL,
                    categories TEXT NOT NULL, itemNames TEXT NOT NULL,
                    createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, synced INTEGER NOT NULL DEFAULT 0)""".trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_outfit_feedback_ownerUid_outfitId ON outfit_feedback(ownerUid, outfitId)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS trips (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    destination TEXT NOT NULL,
                    departureDate TEXT NOT NULL,
                    returnDate TEXT NOT NULL,
                    weatherPreference TEXT NOT NULL,
                    occasion TEXT NOT NULL,
                    packingListJson TEXT NOT NULL,
                    itineraryJson TEXT NOT NULL,
                    ownerUid TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL
                )""".trimIndent())
                db.execSQL("ALTER TABLE outfit_feedback ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE outfits ADD COLUMN colorCompatibilityExplanation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE outfits ADD COLUMN occasionSuitability TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN destinationLat REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE trips ADD COLUMN destinationLng REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE trips ADD COLUMN repackingAlert TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN originLat REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE trips ADD COLUMN originLng REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE trips ADD COLUMN travelDistanceKm REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE trips ADD COLUMN travelDurationHours REAL DEFAULT NULL")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN transportMode TEXT NOT NULL DEFAULT 'Car'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wearwise_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .addMigrations(MIGRATION_7_8)
                    .addMigrations(MIGRATION_8_9)
                    .addMigrations(MIGRATION_9_10)
                    .addMigrations(MIGRATION_10_11)
                    .addMigrations(MIGRATION_11_12)
                    .addMigrations(MIGRATION_12_13)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
