package com.example.data.mapper

import com.example.data.local.entity.ClothingItemEntity
import com.example.data.local.entity.OutfitEntity
import com.example.data.local.entity.OutfitFeedbackEntity
import com.example.data.local.entity.TryOnPreviewEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeeklyScheduleEntity
import com.example.data.local.entity.WearHistoryEntity
import com.example.data.local.entity.ShoppingItemEntity
import com.example.data.local.entity.ShoppingBudgetEntity
import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.repository.OutfitFeedback
import com.example.domain.model.TryOnPreview
import com.example.domain.model.UserProfile
import com.example.domain.model.WeeklyScheduleItem
import com.example.domain.model.WearHistory
import com.example.domain.model.ShoppingItem
import com.example.domain.model.ShoppingBudget

fun ShoppingItemEntity.toDomain() = ShoppingItem(
    id = id,
    remoteId = remoteId,
    ownerUid = ownerUid,
    name = name,
    category = category,
    estimatedPrice = estimatedPrice,
    currency = currency,
    shoppingUrl = shoppingUrl,
    reason = reason,
    priority = priority,
    status = status,
    favorite = favorite,
    quantity = quantity,
    actualPrice = actualPrice,
    purchasedAt = purchasedAt,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ShoppingItem.toEntity() = ShoppingItemEntity(
    id = id,
    remoteId = remoteId,
    ownerUid = ownerUid,
    name = name,
    category = category,
    estimatedPrice = estimatedPrice,
    currency = currency,
    shoppingUrl = shoppingUrl,
    reason = reason,
    priority = priority,
    status = status,
    favorite = favorite,
    quantity = quantity,
    actualPrice = actualPrice,
    purchasedAt = purchasedAt,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ShoppingBudgetEntity.toDomain() = ShoppingBudget(
    amount = amount,
    currency = currency,
    updatedAt = updatedAt
)

fun ShoppingBudget.toEntity(ownerUid: String) = ShoppingBudgetEntity(
    ownerUid = ownerUid,
    amount = amount,
    currency = currency,
    updatedAt = updatedAt
)

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
        colorSeason = colorSeason,
        skinUndertone = skinUndertone,
        hairColorAnalyzed = hairColorAnalyzed,
        eyeColorAnalyzed = eyeColorAnalyzed,
        recommendedColors = if (recommendedColors.isEmpty()) emptyList() else recommendedColors.split(","),
        colorsToAvoid = if (colorsToAvoid.isEmpty()) emptyList() else colorsToAvoid.split(","),
        colorAnalysisRationale = colorAnalysisRationale,
        colorAnalysisPhotoPath = colorAnalysisPhotoPath,
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
        colorSeason = colorSeason,
        skinUndertone = skinUndertone,
        hairColorAnalyzed = hairColorAnalyzed,
        eyeColorAnalyzed = eyeColorAnalyzed,
        recommendedColors = recommendedColors.joinToString(","),
        colorsToAvoid = colorsToAvoid.joinToString(","),
        colorAnalysisRationale = colorAnalysisRationale,
        colorAnalysisPhotoPath = colorAnalysisPhotoPath,
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
        createdAt = createdAt,
        subcategory = subcategory,
        brand = brand,
        size = size,
        occasion = occasion,
        description = description,
        remoteId = remoteId,
        imageStoragePath = imageStoragePath,
        updatedAt = updatedAt,
        imageUrl = imageUrl,
        pattern = pattern,
        ownerUid = ownerUid,
        catalogAssetName = catalogAssetName
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
        createdAt = createdAt,
        subcategory = subcategory,
        brand = brand,
        size = size,
        occasion = occasion,
        description = description,
        remoteId = remoteId,
        imageStoragePath = imageStoragePath,
        updatedAt = updatedAt,
        imageUrl = imageUrl,
        pattern = pattern,
        ownerUid = ownerUid,
        catalogAssetName = catalogAssetName
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
        createdAt = createdAt,
        ownerUid = ownerUid,
        colorCompatibilityExplanation = colorCompatibilityExplanation,
        occasionSuitability = occasionSuitability
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
        createdAt = createdAt,
        ownerUid = ownerUid,
        colorCompatibilityExplanation = colorCompatibilityExplanation,
        occasionSuitability = occasionSuitability
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
        isSavedLook = isSavedLook,
        garmentSummary = garmentSummary,
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
        isSavedLook = isSavedLook,
        garmentSummary = garmentSummary,
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
        isUnscheduled = isUnscheduled,
        dateIso = dateIso,
        outfitId = outfitId,
        occasion = occasion,
        reminderEnabled = reminderEnabled,
        reminderTimeMinutes = reminderTimeMinutes,
        activityTimeMinutes = activityTimeMinutes,
        notes = notes,
        wornAtEpochMillis = wornAtEpochMillis,
        ownerUid = ownerUid
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
        isUnscheduled = isUnscheduled,
        dateIso = dateIso,
        outfitId = outfitId,
        occasion = occasion,
        reminderEnabled = reminderEnabled,
        reminderTimeMinutes = reminderTimeMinutes,
        activityTimeMinutes = activityTimeMinutes,
        notes = notes,
        wornAtEpochMillis = wornAtEpochMillis,
        ownerUid = ownerUid
    )
}

fun WearHistoryEntity.toDomain(): WearHistory = WearHistory(
    id = id,
    outfitId = outfitId,
    outfitTitle = outfitTitle,
    clothingItemIds = clothingItemIds.split(',').mapNotNull { it.toLongOrNull() },
    wornAtEpochMillis = wornAtEpochMillis,
    occasion = occasion,
    ownerUid = ownerUid
)

fun WearHistory.toEntity(): WearHistoryEntity = WearHistoryEntity(
    id = id,
    outfitId = outfitId,
    outfitTitle = outfitTitle,
    clothingItemIds = clothingItemIds.joinToString(","),
    wornAtEpochMillis = wornAtEpochMillis,
    occasion = occasion,
    ownerUid = ownerUid
)

fun OutfitFeedbackEntity.toDomain(): OutfitFeedback = OutfitFeedback(
    feedbackId = feedbackId,
    outfitId = outfitId,
    rating = rating,
    reaction = reaction,
    comment = comment,
    outfitTitle = outfitTitle,
    occasion = occasion,
    style = style,
    colors = colors,
    categories = categories,
    itemNames = itemNames,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun OutfitFeedback.toEntity(ownerUid: String = ""): OutfitFeedbackEntity = OutfitFeedbackEntity(
    feedbackId = feedbackId,
    ownerUid = ownerUid,
    outfitId = outfitId,
    rating = rating,
    reaction = reaction,
    comment = comment,
    outfitTitle = outfitTitle,
    occasion = occasion,
    style = style,
    colors = colors,
    categories = categories,
    itemNames = itemNames,
    createdAt = createdAt,
    updatedAt = updatedAt,
    synced = false,
    deleted = false
)
