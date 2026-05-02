package com.example.markettracker.domain.usecase

import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case: Get the top N coins with the biggest negative price change in 24 hours.
 *
 * This is the business logic for "Top Losers" on the Dashboard screen.
 * Works exactly like [GetTopGainersUseCase] but sorts in the opposite direction.
 *
 * Reads from the same Room DB — no extra network call needed.
 *
 * @param limit How many top losers to return. Default is 5.
 */
class GetTopLosersUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    /**
     * @param limit Max number of losers to return (default 5).
     * @return A [Flow] of coins sorted by biggest price drop, filtered to only negative changers.
     */
    operator fun invoke(limit: Int = 5): Flow<List<Coin>> =
        repository.getCoins().map { coins ->
            coins
                .filter { it.priceChangePercent24h < 0 }    // Only coins that went DOWN
                .sortedBy { it.priceChangePercent24h }       // Biggest loser first (most negative)
                .take(limit)                                  // Keep only top N
        }
}