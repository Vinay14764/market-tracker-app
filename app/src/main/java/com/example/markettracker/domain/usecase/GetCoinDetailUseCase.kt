package com.example.markettracker.domain.usecase

import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case: Get a live stream of a single coin's data by its ID.
 *
 * Used by [CoinDetailViewModel] to power the detail screen with real data
 * instead of the previous hardcoded Bitcoin values.
 *
 * The returned [Flow] emits whenever the coin's data changes in Room
 * (e.g. after a refresh or after the user toggles its favorite status).
 *
 * Returns [Flow<Coin?>] — the value is null if no coin with [coinId] exists in the DB yet.
 * The detail screen should handle the null case by showing a loading state.
 *
 * @param coinId The unique ID of the coin, e.g. "bitcoin", "ethereum".
 *               This comes from the navigation argument in [NavGraph].
 */
class GetCoinDetailUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    /**
     * Returns a [Flow] that emits the [Coin] for the given [coinId],
     * or null if it hasn't been loaded yet.
     */
    operator fun invoke(coinId: String): Flow<Coin?> = repository.getCoinById(coinId)
}