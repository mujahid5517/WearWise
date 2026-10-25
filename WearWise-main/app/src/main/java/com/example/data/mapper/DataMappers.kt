package com.example.data.mapper

import com.example.data.local.entity.ClothingItemEntity
import com.example.data.local.entity.OutfitEntity
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeeklyScheduleEntity
import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.model.TryOnPreview
import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyScheduleItem

fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        firstName = firstName,
        nickname = nickname,
        email = email,
        ageRange = ageRange,
        gender = gender,
        country = country,
        region = region,
        profession = profession,
        dressEnvironment = dressEnvironment,
        stylePreferences = if (stylePreferences.isEmpty()) emptyList() else stylePreferences.split(","),
        favoriteColors = if (favoriteColors.isEmpty()) emptyList() else favoriteColors.split(","),
        avoidedColors = if (avoidedColors.isEmpty()) emptyList() else avoidedColors.split(","),
        commonOccasions = if (commonOccasions.isEmpty()) emptyList() else commonOccasions.split(","),
        climate = climate,
        silhouetteArchetype = silhouetteArchetype,
        profileImage = profileImage,
        profileImageRes = profileImageRes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        id = id,
        firstName = firstName,
        nickname = nickname,
        email = email,
        ageRange = ageRange,
        gender = gender,
        country = country,
        region = region,
        profession = profession,
        dressEnvironment = dressEnvironment,
        stylePreferences = stylePreferences.joinToString(","),
        favoriteColors = favoriteColors.joinToString(","),
        avoidedColors = avoidedColors.joinToString(","),
        commonOccasions = commonOccasions.joinToString(","),
        climate = climate,
        silhouetteArchetype = silhouetteArchetype,
        profileImage = profileImage,
        profileImageRes = profileImageRes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ClothingItemEntity.toDomain(): ClothingItem {
    return ClothingItem(
        id = id,
        userId = userId,
        name = name,
        category = category,
        color = color,
        style = style,
        season = season,
        formality = formality,
        formalityScore = formalityScore,
        fabric = fabric,
        isFavorite = isFavorite,
        imageRes = imageRes,
        imageUri = imageUri,
        wearCount = wearCount,
        lastWornDate = lastWornDate,
        createdAt = createdAt
    )
}

fun ClothingItem.toEntity(): ClothingItemEntity {
    return ClothingItemEntity(
        id = id,
        userId = userId,
        name = name,
        category = category,
        color = color,
        style = style,
        season = season,
        formality = formality,
        formalityScore = formalityScore,
        fabric = fabric,
        isFavorite = isFavorite,
        imageRes = imageRes,
        imageUri = imageUri,
        wearCount = wearCount,
        lastWornDate = lastWornDate,
        createdAt = createdAt
    )
}

fun OutfitEntity.toDomain(items: List<ClothingItem> = emptyList()): Outfit {
    return Outfit(
        id = id,
        userId = userId,
        title = title,
        occasion = occasion,
        style = style,
        score = score,
        rationale = rationale,
        weatherTag = weatherTag,
        isFavorite = isFavorite,
        items = items,
        previewImageRes = previewImageRes,
        previewImageUri = previewImageUri,
        occasionFitScore = occasionFitScore,
        thermalSyncScore = thermalSyncScore,
        colorHarmonyScore = colorHarmonyScore,
        createdAt = createdAt
    )
}

fun Outfit.toEntity(): OutfitEntity {
    return OutfitEntity(
        id = id,
        userId = userId,
        title = title,
        occasion = occasion,
        style = style,
        score = score,
        rationale = rationale,
        weatherTag = weatherTag,
        isFavorite = isFavorite,
        previewImageRes = previewImageRes,
        previewImageUri = previewImageUri,
        occasionFitScore = occasionFitScore,
        thermalSyncScore = thermalSyncScore,
        colorHarmonyScore = colorHarmonyScore,
        createdAt = createdAt
    )
}

fun TryOnPreviewEntity.toDomain(): TryOnPreview {
    return TryOnPreview(
        id = id,
        userId = userId,
        outfitId = outfitId,
        userPhotoPath = userPhotoPath,
        generatedImagePath = generatedImagePath,
        previewImageRes = previewImageRes,
        harmonyScore = harmonyScore,
        fitType = fitType,
        pose = pose,
        lighting = lighting,
        activePiecesCount = activePiecesCount,
        createdAt = createdAt
    )
}

fun TryOnPreview.toEntity(): TryOnPreviewEntity {
    return TryOnPreviewEntity(
        id = id,
        userId = userId,
        outfitId = outfitId,
        userPhotoPath = userPhotoPath,
        generatedImagePath = generatedImagePath,
        previewImageRes = previewImageRes,
        harmonyScore = harmonyScore,
        fitType = fitType,
        pose = pose,
        lighting = lighting,
        activePiecesCount = activePiecesCount,
        createdAt = createdAt
    )
}

fun WeeklyScheduleEntity.toDomain(): WeeklyScheduleItem {
    return WeeklyScheduleItem(
        id = id,
        dayOfWeek = dayOfWeek,
        dateDay = dateDay,
        title = title,
        weatherForecast = weatherForecast,
        formalityBadge = formalityBadge,
        outfitTitle = outfitTitle,
        outfitSubtitle = outfitSubtitle,
        outfitImageRes = outfitImageRes,
        isToday = isToday,
        isWorn = isWorn,
        isUnscheduled = isUnscheduled
    )
}

fun WeeklyScheduleItem.toEntity(): WeeklyScheduleEntity {
    return WeeklyScheduleEntity(
        id = id,
        dayOfWeek = dayOfWeek,
        dateDay = dateDay,
        title = title,
        weatherForecast = weatherForecast,
        formalityBadge = formalityBadge,
        outfitTitle = outfitTitle,
        outfitSubtitle = outfitSubtitle,
        outfitImageRes = outfitImageRes,
        isToday = isToday,
        isWorn = isWorn,
        isUnscheduled = isUnscheduled
    )
}
