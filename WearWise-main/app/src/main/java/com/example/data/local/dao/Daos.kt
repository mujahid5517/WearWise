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
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    fun getUserProfile(id: Long = 1L): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)
}

@Dao
interface ClothingItemDao {
    @Query("SELECT * FROM clothing_items ORDER BY id ASC")
    fun getAllClothingItems(): Flow<List<ClothingItemEntity>>

    @Query("SELECT * FROM clothing_items WHERE category = :category ORDER BY id ASC")
    fun getClothingItemsByCategory(category: String): Flow<List<ClothingItemEntity>>

    @Query("SELECT * FROM clothing_items WHERE id = :id LIMIT 1")
    suspend fun getClothingItemById(id: Long): ClothingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClothingItem(item: ClothingItemEntity): Long

    @Update
    suspend fun updateClothingItem(item: ClothingItemEntity)

    @Query("DELETE FROM clothing_items WHERE id = :id")
    suspend fun deleteClothingItemById(id: Long)

    @Query("UPDATE clothing_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

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

    @Query("SELECT COUNT(*) FROM outfits")
    suspend fun countOutfits(): Int
}

@Dao
interface TryOnPreviewDao {
    @Query("SELECT * FROM try_on_previews ORDER BY createdAt DESC")
    fun getAllTryOnPreviews(): Flow<List<TryOnPreviewEntity>>

    @Query("SELECT * FROM try_on_previews WHERE id = :id LIMIT 1")
    suspend fun getTryOnPreviewById(id: Long): TryOnPreviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTryOnPreview(preview: TryOnPreviewEntity): Long
}

@Dao
interface WeeklyScheduleDao {
    @Query("SELECT * FROM weekly_schedules ORDER BY id ASC")
    fun getWeeklySchedule(): Flow<List<WeeklyScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<WeeklyScheduleEntity>)

    @Update
    suspend fun updateSchedule(schedule: WeeklyScheduleEntity)

    @Query("SELECT COUNT(*) FROM weekly_schedules")
    suspend fun countSchedules(): Int
}
