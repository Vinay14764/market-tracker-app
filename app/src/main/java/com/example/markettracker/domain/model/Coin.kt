package com.example.markettracker.domain.model

import androidx.compose.runtime.Immutable


/**
 * The central domain model for a cryptocurrency coin.
 *
 * This is a plain Kotlin data class with NO external annotations (no @Entity, no @SerializedName).
 * It lives in the DOMAIN layer, which is the middle layer of Clean Architecture.
 *
 * WHY a separate domain model?
 *   - [CoinEntity] (Room) and [CoinDto] (Retrofit) are implementation details.
 *     If you rename a database column or an API JSON field, you only update the mapper.
 *     The ViewModel, use cases, and UI screens never need to change.
 *   - Makes the code fully testable — no Room or network dependency in business logic.
 *
 * Flow:
 *   CoinDto (API) → Mapper → CoinEntity (Room) → Mapper → Coin (Domain) → ViewModel → UI
 */
@Immutable
data class Coin(

    /** Unique ID from CoinGecko, e.g. "bitcoin", "ethereum", "dogecoin" */
    val id: String,

    /** Full display name, e.g. "Bitcoin", "Ethereum" */
    val name: String,

    /** Short ticker symbol, e.g. "btc", "eth". Usually shown in uppercase. */
    val symbol: String,

    /** URL to the coin's logo image. Loaded by Coil in the UI. */
    val imageUrl: String,

    /** Current price in the selected currency (default: INR). */
    val currentPrice: Double,

    /**
     * Price change percentage over the last 24 hours.
     * Positive value = price went up. Negative = price went down.
     * Used to determine the color (green/red) shown next to the price.
     */
    val priceChangePercent24h: Double,

    /**
     * Whether this coin is in the user's favorites list.
     * Stored locally in Room — survives app restarts.
     * NOT a field from the API; it's user-specific local data.
     */
    val isFavorite: Boolean = false,
    val sparkline: List<Float> = emptyList()
)