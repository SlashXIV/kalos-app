package com.kalos.app.core.data.mapper

import com.kalos.app.core.data.util.normalizeForSearch
import com.kalos.app.core.database.entity.FoodEntity
import com.kalos.app.core.domain.model.Food

fun FoodEntity.toDomain() = Food(
    id = id,
    name = name,
    brand = brand,
    category = category,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    defaultServingG = defaultServingG,
    servingUnit = servingUnit,
    isCustom = isCustom,
    isFavorite = isFavorite,
    lastUsedAt = lastUsedAt,
    tags = if (tags.isBlank()) emptyList() else tags.split(","),
    isArchived = isArchived,
    barcode = barcode,
)

fun Food.toEntity() = FoodEntity(
    id = id,
    name = name,
    nameNormalized = name.normalizeForSearch(),
    brand = brand,
    category = category,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    defaultServingG = defaultServingG,
    servingUnit = servingUnit,
    isCustom = isCustom,
    isFavorite = isFavorite,
    lastUsedAt = lastUsedAt,
    tags = tags.joinToString(","),
    isArchived = isArchived,
    barcode = barcode,
)
