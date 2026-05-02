package com.example.markettracker.domain.usecase

import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case: Get the live stream of all coins from local storage (Room DB).
 *
 * WHY a use case?
 *   - Single responsibility: this class does ONE thing — expose the coin list as a Flow.
 *   - The ViewModel doesn't need to know whether coins come from Room, network, or a cache.
 *   - Easy to test: just inject a fake [ICoinRepository].
 *
 * Usage in ViewModel:
 *   val coins: Flow<List<Coin>> = getCoins()
 *
 * The [invoke] operator allows calling it like a function: getCoins() instead of getCoins.invoke()
 */
class GetCoinsUseCase @Inject constructor(
    private val repository: CoinRepository
) {
    /**
     * Returns a [Flow] that emits the full list of [Coin]s from the local database.
     * The flow automatically re-emits whenever the database is updated.
     * Room handles this reactivity internally.
     */
    operator fun invoke(): Flow<List<Coin>> = repository.getCoins()
}