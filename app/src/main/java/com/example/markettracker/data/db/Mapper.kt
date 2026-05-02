package com.example.markettracker.data.db

import com.example.markettracker.data.remote.dto.CoinDto
import com.example.markettracker.domain.model.Coin
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Mapper functions — convert between the three data model types.
 *
 * Clean Architecture uses different models for different layers:
 *   [CoinDto]    → the raw API response from Retrofit (has @SerializedName annotations)
 *   [CoinEntity] → the Room database model (has @Entity, @PrimaryKey annotations)
 *   [Coin]       → the pure domain model (no annotations, used in ViewModels and UI)
 *
 * These mapper functions handle all conversions between them. If any field is
 * renamed in the API or DB, you only update the mapper — nothing else changes.
 *
 * Extension functions let you call them naturally:
 *   coinDto.toEntity()    → CoinEntity (to save to Room)
 *   coinEntity.toDomain() → Coin (to pass to ViewModel)
 */

// ─────────────────────────────────────────────────
// API Response (CoinDto) → Database Entity (CoinEntity)
// ─────────────────────────────────────────────────

/**
 * Converts a [CoinDto] (from Retrofit/API) into a [CoinEntity] (for Room storage).
 *
 * Note: [isFavorite] is always set to false here because the API doesn't know
 * about the user's favorites. Room's REPLACE strategy preserves the existing
 * [isFavorite] value... actually REPLACE drops it. So the repository should
 * read the existing favorite state before inserting. For now, the DAO uses
 * OnConflictStrategy.REPLACE which resets favorites on refresh.
 * TODO: Use a smarter upsert that preserves isFavorite on conflict.
 */
fun CoinDto.toEntity(): CoinEntity = CoinEntity(
    id           = id,
    name         = name,
    symbol       = symbol,
    image        = image,
    price        = currentPrice,
    change       = priceChangePercentage24h,
    // Serialise the sparkline price list to JSON so it can be stored as a single
    // text column. Gson is already on the classpath via converter-gson.
    sparklineJson = Gson().toJson(sparklineIn7d.price)
    // isFavorite defaults to false (Room REPLACE will overwrite the existing row)
)

// ─────────────────────────────────────────────────
// Database Entity (CoinEntity) → Domain Model (Coin)
// ─────────────────────────────────────────────────

/**
 * Converts a [CoinEntity] (from Room) into a [Coin] (domain model for ViewModels/UI).
 *
 * Called in [CoinRepositoryImpl] when mapping the Room Flow to a domain Flow.
 * This is the most-used mapper — every screen that shows coins goes through this.
 */
fun CoinEntity.toDomain(): Coin = Coin(
    id                    = id,
    name                  = name,
    symbol                = symbol,
    imageUrl              = image,
    currentPrice          = price,
    priceChangePercent24h = change,
    isFavorite            = isFavorite,
    // Deserialise the JSON string back to a List<Double>, then convert each
    // value to Float so it matches the Coin.sparkline type.
    sparkline = Gson().fromJson<List<Double>>(
        sparklineJson,
        object : TypeToken<List<Double>>() {}.type
    )?.map { it.toFloat() } ?: emptyList()
)
fun CoinDto.toDomain(): Coin = Coin(
    id = id,
    name = name,
    symbol = symbol,
    imageUrl = image,
    currentPrice = currentPrice,
    priceChangePercent24h = priceChangePercentage24h,
    isFavorite = false,
    sparkline = sparklineIn7d?.price?.map { it.toFloat() } ?: emptyList()
)

