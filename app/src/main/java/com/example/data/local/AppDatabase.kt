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
import com.example.data.local.entity.ClothingItemEntity
import com.example.data.local.entity.OutfitEntity
import com.example.data.local.entity.OutfitItemEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeeklyScheduleEntity
import com.example.data.local.entity.WearHistoryEntity
import com.example.data.local.entity.ShoppingItemEntity

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
        ShoppingItemEntity::class
    ],
    version = 10,
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
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
