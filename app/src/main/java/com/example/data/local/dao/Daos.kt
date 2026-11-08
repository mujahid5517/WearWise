package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ClothingItemEntity
import com.example.data.local.entity.OutfitEntity
import com.example.data.local.entity.OutfitItemEntity
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeeklyScheduleEntity
import com.example.data.local.entity.WearHistoryEntity
import com.example.data.local.entity.ShoppingItemEntity
import com.example.data.local.entity.TripEntity
import com.example.data.local.entity.OutfitFeedbackEntity
import com.example.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE ownerUid = :ownerUid AND id = :id LIMIT 1")
    fun getUserProfile(ownerUid: String, id: Long = 1L): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE ownerUid = :ownerUid AND id = :id LIMIT 1")
    suspend fun getUserProfileOnce(ownerUid: String, id: Long = 1L): UserProfileEntity?

    @Query("SELECT * FROM user_profiles")
    fun observeProfiles(): Flow<List<UserProfileEntity>>

    @Query("""
        UPDATE user_profiles
        SET ownerUid = :ownerUid
        WHERE id = 1
            AND ownerUid = ''
            AND NOT EXISTS (
                SELECT 1 FROM user_profiles
                WHERE id = 1 AND ownerUid = :ownerUid
            )
    """)
    suspend fun claimLegacyProfile(ownerUid: String)

    @Query("DELETE FROM user_profiles WHERE ownerUid = :ownerUid")
    suspend fun deleteProfile(ownerUid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)
}

@Dao
interface ClothingItemDao {
    @Query("SELECT * FROM clothing_items ORDER BY id ASC")
    fun getAllClothingItems(): Flow<List<ClothingItemEntity>>

    @Query("SELECT * FROM clothing_items ORDER BY id ASC")
    suspend fun getAllClothingItemsSnapshot(): List<ClothingItemEntity>

    @Query("SELECT * FROM clothing_items WHERE category = :category ORDER BY id ASC")
    fun getClothingItemsByCategory(category: String): Flow<List<ClothingItemEntity>>

    @Query("SELECT * FROM clothing_items WHERE id = :id LIMIT 1")
    suspend fun getClothingItemById(id: Long): ClothingItemEntity?

    @Query("SELECT * FROM clothing_items WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getClothingItemByRemoteId(remoteId: String): ClothingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClothingItem(item: ClothingItemEntity): Long

    @Update
    suspend fun updateClothingItem(item: ClothingItemEntity)

    @Query("DELETE FROM clothing_items WHERE id = :id")
    suspend fun deleteClothingItemById(id: Long)

    @Query("UPDATE clothing_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE clothing_items SET wearCount = wearCount + 1, lastWornDate = :lastWornDate WHERE id = :id")
    suspend fun markWorn(id: Long, lastWornDate: String)

    @Query("SELECT COUNT(*) FROM clothing_items")
    suspend fun countItems(): Int
}

@Dao
interface OutfitDao {
    @Query("SELECT * FROM outfits ORDER BY createdAt DESC")
    fun getAllOutfits(): Flow<List<OutfitEntity>>

    @Query("SELECT * FROM outfits WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteOutfits(): Flow<List<OutfitEntity>>

    @Query("SELECT * FROM outfits WHERE id = :id LIMIT 1")
    suspend fun getOutfitById(id: Long): OutfitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(outfit: OutfitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfitItems(items: List<OutfitItemEntity>)

    @Query("SELECT clothingId FROM outfit_items WHERE outfitId = :outfitId")
    suspend fun getClothingIdsForOutfit(outfitId: Long): List<Long>

    @Query("UPDATE outfits SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM outfits WHERE id = :id")
    suspend fun deleteOutfitById(id: Long)

    @Query("DELETE FROM outfit_items WHERE outfitId = :outfitId")
    suspend fun deleteOutfitItemsByOutfitId(outfitId: Long)

    @Query("SELECT COUNT(*) FROM outfits")
    suspend fun countOutfits(): Int
}

@Dao
interface TryOnPreviewDao {
    @Query("SELECT * FROM try_on_previews ORDER BY createdAt DESC")
    fun getAllTryOnPreviews(): Flow<List<TryOnPreviewEntity>>

    @Query("SELECT * FROM try_on_previews WHERE id = :id LIMIT 1")
    suspend fun getTryOnPreviewById(id: Long): TryOnPreviewEntity?

    @Query("SELECT generatedImagePath FROM try_on_previews WHERE outfitId = :outfitId AND fitType = 'Catalog' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestCatalogImage(outfitId: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTryOnPreview(preview: TryOnPreviewEntity): Long

    @Query("DELETE FROM try_on_previews WHERE id = :id")
    suspend fun deleteTryOnPreviewById(id: Long)

    @Query("UPDATE try_on_previews SET isSavedLook = :saved WHERE id = :id")
    suspend fun setSavedLook(id: Long, saved: Boolean)
}

@Dao
interface WeeklyScheduleDao {
    @Query("SELECT * FROM weekly_schedules ORDER BY id ASC")
    fun getWeeklySchedule(): Flow<List<WeeklyScheduleEntity>>

    @Query("SELECT * FROM weekly_schedules WHERE ownerUid = :ownerUid")
    suspend fun getAllForOwner(ownerUid: String): List<WeeklyScheduleEntity>

    @Query("SELECT * FROM weekly_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): WeeklyScheduleEntity?

    @Query("SELECT * FROM weekly_schedules WHERE dateIso = :dateIso AND ownerUid = :ownerUid LIMIT 1")
    suspend fun getScheduleForDate(dateIso: String, ownerUid: String): WeeklyScheduleEntity?

    @Query("DELETE FROM weekly_schedules WHERE dateIso = :dateIso AND ownerUid = :ownerUid")
    suspend fun deleteScheduleForDate(dateIso: String, ownerUid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<WeeklyScheduleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: WeeklyScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: WeeklyScheduleEntity)

    @Query("DELETE FROM weekly_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    @Query("SELECT COUNT(*) FROM weekly_schedules")
    suspend fun countSchedules(): Int
}

@Dao
interface WearHistoryDao {
    @Query("SELECT * FROM wear_history WHERE ownerUid = :ownerUid ORDER BY wornAtEpochMillis DESC")
    fun observe(ownerUid: String): Flow<List<WearHistoryEntity>>

    @Query("SELECT * FROM wear_history WHERE ownerUid = :ownerUid ORDER BY wornAtEpochMillis DESC LIMIT :limit")
    suspend fun recent(ownerUid: String, limit: Int = 30): List<WearHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: WearHistoryEntity): Long
}

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items")
    suspend fun snapshot(): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ShoppingItemEntity?

    @Query("SELECT * FROM shopping_items WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ShoppingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity): Long

    @Update
    suspend fun update(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity): Long

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface OutfitFeedbackDao {
    @Query("SELECT * FROM outfit_feedback WHERE ownerUid = :uid AND deleted = 0 ORDER BY updatedAt DESC")
    fun observe(uid: String): Flow<List<OutfitFeedbackEntity>>
    @Query("SELECT * FROM outfit_feedback WHERE ownerUid = :uid AND outfitId = :outfitId LIMIT 1")
    suspend fun get(uid: String, outfitId: Long): OutfitFeedbackEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: OutfitFeedbackEntity): Long
    @Query("DELETE FROM outfit_feedback WHERE ownerUid = :uid AND outfitId = :outfitId")
    suspend fun delete(uid: String, outfitId: Long)
    @Query("SELECT * FROM outfit_feedback WHERE synced = 0 AND ownerUid = :uid")
    suspend fun pending(uid: String): List<OutfitFeedbackEntity>
    @Query("UPDATE outfit_feedback SET synced = 1 WHERE feedbackId = :id")
    suspend fun markSynced(id: Long)
    @Query("UPDATE outfit_feedback SET deleted = 1, synced = 0 WHERE ownerUid = :uid AND outfitId = :outfitId")
    suspend fun markDeleted(uid: String, outfitId: Long)
    @Query("DELETE FROM outfit_feedback WHERE feedbackId = :id")
    suspend fun purge(id: Long)
}
