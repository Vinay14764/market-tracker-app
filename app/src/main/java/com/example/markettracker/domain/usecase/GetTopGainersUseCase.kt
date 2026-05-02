package com.example.markettracker.domain.usecase

import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case: Get the top N coins with the highest positive price change in 24 hours.
 *
 * This is the business logic for "Top Gainers" on the Dashboard screen.
 * Previously, the Dashboard showed hardcoded data like "Bitcoin +3.43%".
 * Now it shows REAL live data sourced from the same Room DB that the Market screen uses.
 *
 * No extra network call is needed here — MarketScreen's refresh already populated the DB.
 * Room emits the updated list reactively, so this flow stays current automatically.
 *
 * @param limit How many top gainers to return. Default is 5.
 */
class GetTopGainersUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    /**
     * @param limit Max number of gainers to return (default 5).
     * @return A [Flow] of coins sorted by biggest price increase, filtered to only positive changers.
     */
    operator fun invoke(limit: Int = 5): Flow<List<Coin>> =
        repository.getCoins().map { coins ->
            coins
                .filter { it.priceChangePercent24h > 0 }           // Only coins that went UP
                .sortedByDescending { it.priceChangePercent24h }    // Biggest gainer first
                .take(limit)                                         // Keep only top N
        }
}